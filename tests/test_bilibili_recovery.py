"""Offline engine regressions; no Bilibili login or public CDN required."""
import copy
import hashlib
import importlib
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit

import yt_dlp

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'app/src/main/assets/yt-dlp-plugins/kirin'))
plugin = importlib.import_module('yt_dlp_plugins.extractor.kirin_bilibili')


class QuietLogger:
    def debug(self, message):
        pass

    info = warning = error = debug


def video_info(primary, backups):
    return {'dash': {'video': [{
        'id': 80, 'baseUrl': primary, 'backupUrl': backups,
        'mimeType': 'video/mp4', 'codecs': 'avc1.640028',
        'width': 160, 'height': 90, 'frameRate': '5', 'bandwidth': 40000,
    }]}}


class BilibiliMirrorTests(unittest.TestCase):
    def extract(self, play_info, attempt):
        with yt_dlp.YoutubeDL({'quiet': True, 'logger': QuietLogger(),
                              'extractor_args': {'bilibili': {'kirin_cdn_attempt': [str(attempt)]}}}) as ydl:
            return plugin._KirinBiliBiliIE(ydl).extract_formats(play_info)

    def test_signed_backup_keeps_format_and_original_response(self):
        primary = 'https://main.example/42627433138-1-100026.m4s?sig=primary'
        backup = 'https://backup.example/other-name.m4s?sig=keep%2Bexact&deadline=123'
        info = video_info(primary, [primary, 'file:///invalid', backup, backup])
        original = copy.deepcopy(info)
        first = self.extract(info, 0)[0]
        recovered = self.extract(info, 1)[0]
        self.assertEqual(recovered['url'], backup)
        self.assertEqual(recovered['format_id'], '100026')
        self.assertEqual({k: v for k, v in first.items() if k != 'url'},
                         {k: v for k, v in recovered.items() if k != 'url'})
        self.assertEqual(info, original)

    def test_missing_mirror_never_invents_a_host(self):
        primary = 'https://main.example/42627433138-1-100026.m4s'
        self.assertEqual(self.extract(video_info(primary, []), 1)[0]['url'], primary)

    def test_snake_case_dolby_flac_and_fragment_backups(self):
        info = {'dash': {'audio': [{'base_url': 'https://a/main', 'backup_url': ['https://a/backup']}],
                         'dolby': {'audio': [{'baseUrl': 'https://d/main', 'backupUrl': ['https://d/backup']}]},
                         'flac': {'audio': {'url': 'https://f/main', 'backup_url': ['https://f/backup']}}},
                'durl': [{'url': 'https://v/one', 'backup_url': ['https://v/one-backup']},
                         {'url': 'https://v/two', 'backup_url': ['https://v/two-backup']}]}
        mirrors = plugin._mirror_map(info)
        self.assertEqual(len(mirrors), 5)
        formats = [{'url': 'https://v/one', 'fragments': [{'url': 'https://v/one'}, {'url': 'https://v/two'}]}]
        plugin._apply_mirrors(formats, mirrors, 1)
        self.assertEqual([f['url'] for f in formats[0]['fragments']],
                         ['https://v/one-backup', 'https://v/two-backup'])

    def test_cli_loads_bundled_plugin(self):
        script = """
from yt_dlp.globals import plugin_dirs
from yt_dlp.plugins import load_all_plugins
from yt_dlp import YoutubeDL
import sys
plugin_dirs.value = [sys.argv[1]]
load_all_plugins()
from yt_dlp.extractor import get_info_extractor
with YoutubeDL({'quiet': True, 'extractor_args': {'bilibili': {'kirin_cdn_attempt': ['1']}}}) as ydl:
    ie = get_info_extractor('BiliBili')(ydl)
    formats = ie.extract_formats({'dash': {'video': [{'id': 80, 'baseUrl': 'https://a.test/1-100026.m4s', 'backupUrl': ['https://b.test/1-100026.m4s']}]}})
    assert formats[0]['url'].startswith('https://b.test/'), formats
"""
        subprocess.run([sys.executable, '-c', script, str(ROOT / 'app/src/main/assets/yt-dlp-plugins')],
                       check=True, capture_output=True, text=True)

    @unittest.skipUnless(shutil.which('ffmpeg') and shutil.which('ffprobe'), 'FFmpeg required')
    def test_interrupted_video_resumes_on_mirror_and_merges_with_completed_audio(self):
        with tempfile.TemporaryDirectory() as directory:
            temp = pathlib.Path(directory)
            subprocess.run(['ffmpeg', '-loglevel', 'error', '-f', 'lavfi', '-i',
                            'color=c=blue:s=160x90:r=5', '-t', '1', '-c:v', 'libx264',
                            '-an', str(temp / 'video.mp4')], check=True)
            subprocess.run(['ffmpeg', '-loglevel', 'error', '-f', 'lavfi', '-i',
                            'sine=frequency=440:sample_rate=44100', '-t', '1', '-c:a', 'aac',
                            '-vn', str(temp / 'audio.m4a')], check=True)
            video = (temp / 'video.mp4').read_bytes()
            audio = (temp / 'audio.m4a').read_bytes()
            requests = []

            class Handler(BaseHTTPRequestHandler):
                def log_message(self, *args):
                    pass

                def do_GET(self):
                    path = urlsplit(self.path).path
                    data = audio if path.startswith('/audio') else video
                    start = int(self.headers.get('Range', 'bytes=0-').split('=')[1].split('-')[0])
                    requests.append((path, start))
                    self.send_response(206 if start else 200)
                    self.send_header('Content-Length', str(len(data) - start))
                    self.send_header('Content-Type', 'audio/mp4' if path.startswith('/audio') else 'video/mp4')
                    self.send_header('Accept-Ranges', 'bytes')
                    if start:
                        self.send_header('Content-Range', f'bytes {start}-{len(data)-1}/{len(data)}')
                    self.end_headers()
                    end = max(start, len(data) // 2) if path.startswith('/primary') else len(data)
                    self.wfile.write(data[start:end])
                    self.wfile.flush()
                    self.close_connection = True

            server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                base = f'http://127.0.0.1:{server.server_port}'
                info = video_info(base + '/primary/42627433138-1-100026.m4s?signature=primary',
                                  [base + '/backup/42627433138-1-100026.m4s?signature=untouched'])
                info['dash']['video'][0]['size'] = len(video)
                info['dash']['audio'] = [{'id': 30280, 'baseUrl': base + '/audio/30280.m4s',
                                         'mimeType': 'audio/mp4', 'codecs': 'mp4a.40.2',
                                         'bandwidth': 128000, 'size': len(audio)}]
                options = {'outtmpl': str(temp / 'result.%(ext)s'), 'format': '100026+30280',
                           'retries': 0, 'continuedl': True, 'keepvideo': True,
                           'ignoreerrors': 'only_download',  # Match the app's yt-dlp CLI default.
                           'quiet': True, 'logger': QuietLogger(), 'noprogress': True}

                def download(attempt):
                    with yt_dlp.YoutubeDL(options) as ydl:
                        ydl.process_ie_result({'id': 'test', 'title': 'test',
                                               'extractor': 'BiliBili', 'extractor_key': 'BiliBili',
                                               'formats': self.extract(info, attempt)})
                        return ydl._download_retcode

                self.assertNotEqual(download(0), 0)
                self.assertTrue((temp / 'result.f100026.mp4.part').exists())
                self.assertTrue((temp / 'result.f30280.m4a').exists())
                self.assertFalse((temp / 'result.mp4').exists())
                self.assertEqual(download(1), 0)
                self.assertEqual(hashlib.sha256((temp / 'result.f100026.mp4').read_bytes()).digest(),
                                 hashlib.sha256(video).digest())
                self.assertEqual(sum(path.startswith('/audio') for path, _ in requests), 1)
                self.assertTrue(any(path.startswith('/backup') and start > 0 for path, start in requests))
                report = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams',
                                                             '-of', 'json', str(temp / 'result.mp4')]))
                self.assertEqual({s['codec_type'] for s in report['streams']}, {'audio', 'video'})
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == '__main__':
    unittest.main()
