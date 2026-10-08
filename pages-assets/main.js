(() => {
  'use strict';

  const API = 'https://api.github.com/repos/Lanzkila/KirinDL';
  const CACHE_TTL = 60 * 60 * 1000; // One hour; avoids repeat API calls on every visit.
  const MAX_RELEASE_PAGES = 25; // A safety cap. Never display a partial sum as a total.
  const numberFormat = new Intl.NumberFormat('en-US');

  const version = document.getElementById('latest-version');
  const latestLink = document.getElementById('latest-link');
  const downloads = document.getElementById('github-downloads');
  const forks = document.getElementById('github-forks');
  const stars = document.getElementById('github-stars');
  const year = document.getElementById('year');

  if (year) year.textContent = new Date().getFullYear();

  function readCache(key) {
    try {
      const cached = JSON.parse(localStorage.getItem(key) || 'null');
      return cached && typeof cached.savedAt === 'number' ? cached : null;
    } catch (_) {
      return null;
    }
  }

  function saveCache(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(Object.assign({ savedAt: Date.now() }, value)));
    } catch (_) {
      // Private browsing and storage-disabled browsers must still render the site.
    }
  }

  function fresh(cached) {
    return cached && Date.now() - cached.savedAt >= 0 &&
      Date.now() - cached.savedAt < CACHE_TTL;
  }

  function showCount(element, count) {
    if (element && Number.isFinite(count) && count >= 0) {
      element.textContent = numberFormat.format(count);
    }
  }

  async function requestJson(url) {
    const response = await fetch(url, {
      headers: { Accept: 'application/vnd.github+json' }
    });
    if (!response.ok) throw new Error('GitHub API returned HTTP ' + response.status);
    return response.json();
  }

  function showLatest(release) {
    if (!version) return;
    version.textContent = release && (release.tag || release.name)
      ? (release.tag || release.name)
      : 'See Releases';
    if (latestLink && release && release.url &&
      release.url.startsWith('https://github.com/Lanzkila/KirinDL/releases/')) {
      latestLink.href = release.url;
    }
  }

  async function loadRepositoryStats() {
    const cacheKey = 'kirindl-pages-repo-stats-v1';
    const cached = readCache(cacheKey);
    if (cached) {
      showCount(forks, cached.forks);
      showCount(stars, cached.stars);
      if (fresh(cached)) return;
    }

    try {
      const repo = await requestJson(API);
      const data = {
        forks: repo.forks_count,
        stars: repo.stargazers_count
      };
      if (!Number.isSafeInteger(data.forks) || !Number.isSafeInteger(data.stars)) {
        throw new Error('Missing repository statistics');
      }
      showCount(forks, data.forks);
      showCount(stars, data.stars);
      saveCache(cacheKey, data);
    } catch (error) {
      console.warn('KirinDL repository statistics unavailable:', error);
      if (!cached) {
        if (forks) forks.textContent = '—';
        if (stars) stars.textContent = '—';
      }
    }
  }

  async function loadReleaseStats() {
    const cacheKey = 'kirindl-pages-release-stats-v1';
    const cached = readCache(cacheKey);
    if (cached) {
      showCount(downloads, cached.downloads);
      showLatest(cached.latest);
      if (fresh(cached)) return;
    }

    try {
      let totalApkDownloads = 0;
      let latest = null;
      let finished = false;

      // GitHub does not expose a total-release-downloads field. Sum the
      // download_count of .apk assets from every published release, including
      // Pre-releases. Pages are loaded until the final page is reached.
      for (let page = 1; page <= MAX_RELEASE_PAGES; page++) {
        const releases = await requestJson(API + '/releases?per_page=100&page=' + page);
        if (!Array.isArray(releases)) throw new Error('Invalid releases response');

        if (page === 1) {
          const first = releases.find(item => item && !item.draft);
          if (first) {
            latest = {
              tag: first.tag_name || '',
              name: first.name || '',
              url: first.html_url || ''
            };
          }
        }

        for (const release of releases) {
          if (!release || release.draft || !Array.isArray(release.assets)) continue;
          for (const asset of release.assets) {
            if (!asset || !/\.apk$/i.test(asset.name || '')) continue;
            if (Number.isSafeInteger(asset.download_count) && asset.download_count >= 0) {
              totalApkDownloads += asset.download_count;
            }
          }
        }

        if (releases.length < 100) {
          finished = true;
          break;
        }
      }
      if (!finished || !Number.isSafeInteger(totalApkDownloads)) {
        throw new Error('Could not calculate the complete release download total');
      }

      showCount(downloads, totalApkDownloads);
      showLatest(latest);
      saveCache(cacheKey, { downloads: totalApkDownloads, latest });
    } catch (error) {
      console.warn('KirinDL release statistics unavailable:', error);
      if (!cached) {
        if (downloads) downloads.textContent = '—';
        showLatest(null);
      }
    }
  }

  // Independent requests: even if one API endpoint is rate-limited, the
  // other counters can still display cached or fresh data.
  void loadRepositoryStats();
  void loadReleaseStats();
})();