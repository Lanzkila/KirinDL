//<![CDATA[
(() => {
    'use strict';

    const API_URL = 'https://graphql.anilist.co';
    const SLIDE_MS = 7000;
    const CACHE_MS = 30 * 60 * 1000;
    const CACHE_KEY = 'kirinBioAniListFeeds_v2';
    const VISITOR_REFRESH_MS = 15000;


    const discoverSection = document.getElementById('discoverSection');
    const discoverToggle = document.getElementById('discoverToggle');
    if (discoverSection && discoverToggle) {
        discoverToggle.addEventListener('click', () => {
            const isOpen = discoverSection.classList.toggle('is-open');
            discoverToggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        });
    }

    const QUERY = `
      query KirinBioFeed {
        anime: Page(page: 1, perPage: 25) {
          media(type: ANIME, status: RELEASING, isAdult: false, sort: [TRENDING_DESC, POPULARITY_DESC]) {
            id
            title { romaji english native }
            format
            description(asHtml: false)
            genres
            averageScore
            popularity
            siteUrl
            bannerImage
            coverImage { extraLarge large color }
            nextAiringEpisode { airingAt timeUntilAiring episode }
          }
        }
        manga: Page(page: 1, perPage: 10) {
          media(type: MANGA, status: RELEASING, isAdult: false, sort: [TRENDING_DESC, POPULARITY_DESC]) {
            id
            title { romaji english native }
            format
            status
            description(asHtml: false)
            averageScore
            popularity
            chapters
            volumes
            genres
            siteUrl
            bannerImage
            coverImage { extraLarge large color }
          }
        }
      }`;

    const q = (root, selector) => root.querySelector(selector);

    function pickTitle(media) {
        return media?.title?.english || media?.title?.romaji || media?.title?.native || 'Untitled';
    }

    function compactNumber(value) {
        if (!value && value !== 0) return null;
        try { return new Intl.NumberFormat('en', { notation: 'compact', maximumFractionDigits: 1 }).format(value); }
        catch (_) { return String(value); }
    }

    function formatCountdown(seconds) {
        seconds = Math.max(0, Number(seconds || 0));
        const d = Math.floor(seconds / 86400);
        const h = Math.floor((seconds % 86400) / 3600);
        const m = Math.floor((seconds % 3600) / 60);
        if (d > 0) return `${d}d ${h}h`;
        if (h > 0) return `${h}h ${m}m`;
        return `${Math.max(1, m)}m`;
    }

    function cleanFormat(value) {
        return value ? value.replaceAll('_', ' ') : null;
    }

    function loadCache() {
        try {
            const raw = localStorage.getItem(CACHE_KEY);
            if (!raw) return null;
            const parsed = JSON.parse(raw);
            if (!parsed?.savedAt || Date.now() - parsed.savedAt > CACHE_MS) return null;
            return parsed.data || null;
        } catch (_) { return null; }
    }

    function saveCache(data) {
        try { localStorage.setItem(CACHE_KEY, JSON.stringify({ savedAt: Date.now(), data })); }
        catch (_) {}
    }

    async function fetchFeeds() {
        const cached = loadCache();
        if (cached) return cached;

        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify({ query: QUERY })
        });
        if (!response.ok) throw new Error(`AniList request failed: ${response.status}`);
        const payload = await response.json();
        if (payload.errors?.length) throw new Error(payload.errors[0]?.message || 'AniList GraphQL error');
        saveCache(payload.data);
        return payload.data;
    }

    function normalizeAnime(list) {
        return (list || [])
            .filter(item => item && item.nextAiringEpisode && item.siteUrl)
            .sort((a, b) => a.nextAiringEpisode.airingAt - b.nextAiringEpisode.airingAt)
            .slice(0, 10);
    }

    function normalizeManga(list) {
        return (list || []).filter(item => item && item.siteUrl).slice(0, 10);
    }

    function stripMarkup(value) {
        if (!value) return 'No description available.';
        const temp = document.createElement('div');
        temp.innerHTML = String(value).replace(/<br\s*\/?\s*>/gi, '\n');
        return (temp.textContent || temp.innerText || '').replace(/\n{3,}/g, '\n\n').trim() || 'No description available.';
    }

    function buildModalMeta(media, type) {
        const bits = [];
        if (media.format) bits.push(cleanFormat(media.format));
        if (media.averageScore) bits.push(`★ ${media.averageScore}%`);
        if (type === 'anime' && media.nextAiringEpisode) {
            bits.push(`Ep ${media.nextAiringEpisode.episode} in ${formatCountdown(media.nextAiringEpisode.timeUntilAiring)}`);
        }
        if (type === 'manga') {
            if (media.chapters) bits.push(`${media.chapters} chapters`);
            else if (media.volumes) bits.push(`${media.volumes} volumes`);
        }
        if (media.genres?.length) bits.push(...media.genres.slice(0, 2));
        return bits.slice(0, 5);
    }

    function openMediaModal(media, type) {
        const modal = document.getElementById('mediaModal');
        if (!modal) return;
        const cover = document.getElementById('mediaModalCover');
        const title = document.getElementById('mediaModalTitle');
        const typeLabel = document.getElementById('mediaModalType');
        const meta = document.getElementById('mediaModalMeta');
        const description = document.getElementById('mediaModalDescription');
        const open = document.getElementById('mediaModalOpen');
        const card = modal.querySelector('.media-modal-card');

        cover.src = media.coverImage?.extraLarge || media.coverImage?.large || '';
        cover.alt = `${pickTitle(media)} cover`;
        title.textContent = pickTitle(media);
        typeLabel.textContent = type === 'anime' ? 'Anime • Airing Now' : 'Manga • Trending';
        description.textContent = stripMarkup(media.description);
        open.href = media.siteUrl || 'https://anilist.co';
        card?.style.setProperty('--modal-brand', type === 'anime' ? '#69c9ff' : '#ff86ba');

        meta.innerHTML = '';
        buildModalMeta(media, type).forEach(bit => {
            const pill = document.createElement('span');
            pill.textContent = bit;
            meta.appendChild(pill);
        });

        modal.classList.add('is-open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
        modal.querySelector('.media-modal-close')?.focus({ preventScroll: true });
    }

    function closeMediaModal() {
        const modal = document.getElementById('mediaModal');
        if (!modal) return;
        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
    }

    function bindMediaModal() {
        const modal = document.getElementById('mediaModal');
        if (!modal) return;
        modal.addEventListener('click', event => {
            if (event.target.closest('[data-modal-close="true"]')) closeMediaModal();
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && modal.classList.contains('is-open')) closeMediaModal();
        });
    }

    function createMediaLink(media, type) {
        const link = document.createElement('button');
        link.className = 'api-media-link';
        link.type = 'button';
        link.setAttribute('aria-label', `View details for ${pickTitle(media)}`);
        link.addEventListener('click', () => openMediaModal(media, type));

        const copy = document.createElement('div');
        copy.className = 'api-media-copy';

        const kicker = document.createElement('div');
        kicker.className = 'api-kicker';
        if (type === 'anime') {
            kicker.textContent = `Episode ${media.nextAiringEpisode.episode} • in ${formatCountdown(media.nextAiringEpisode.timeUntilAiring)}`;
        } else {
            kicker.textContent = 'Trending • Releasing';
        }

        const title = document.createElement('h2');
        title.className = 'api-title';
        title.textContent = pickTitle(media);

        const meta = document.createElement('div');
        meta.className = 'api-meta';
        const bits = [];
        if (media.format) bits.push(cleanFormat(media.format));
        if (media.averageScore) bits.push(`★ ${media.averageScore}%`);
        const genre = media.genres?.[0];
        if (genre) bits.push(genre);
        bits.slice(0, 3).forEach(bit => {
            const pill = document.createElement('span');
            pill.className = 'api-pill';
            pill.textContent = bit;
            meta.appendChild(pill);
        });

        copy.append(kicker, title, meta);

        const cover = document.createElement('img');
        cover.className = 'api-cover';
        cover.alt = `${pickTitle(media)} cover`;
        cover.loading = 'lazy';
        cover.decoding = 'async';
        cover.src = media.coverImage?.extraLarge || media.coverImage?.large || '';

        link.append(copy, cover);
        return link;
    }

    class MediaSlider {
        constructor(root, items, type) {
            this.root = root;
            this.items = items;
            this.type = type;
            this.index = 0;
            this.timer = null;
            this.touchX = null;
            this.inner = q(root, '.api-slider-inner');
            this.loading = q(root, '.api-loading');
            this.counter = q(root, '.api-counter');
            this.bg = q(root, '.api-slider-bg');
            this.progress = q(root, '.api-progress > span');
            this.bind();
            this.render(0, false);
            this.play();
        }

        bind() {
            q(this.root, '.api-prev').addEventListener('click', () => this.go(-1));
            q(this.root, '.api-next').addEventListener('click', () => this.go(1));
            this.root.addEventListener('mouseenter', () => this.pauseVisual());
            this.root.addEventListener('mouseleave', () => this.resumeVisual());
            this.root.addEventListener('focusin', () => this.pauseVisual());
            this.root.addEventListener('focusout', () => this.resumeVisual());
            this.root.addEventListener('touchstart', e => { this.touchX = e.changedTouches?.[0]?.clientX ?? null; this.pauseVisual(); }, { passive: true });
            this.root.addEventListener('touchend', e => {
                const end = e.changedTouches?.[0]?.clientX;
                if (this.touchX != null && end != null) {
                    const delta = end - this.touchX;
                    if (Math.abs(delta) > 42) this.go(delta < 0 ? 1 : -1);
                }
                this.touchX = null;
                this.resumeVisual();
            }, { passive: true });
            document.addEventListener('visibilitychange', () => {
                if (document.hidden) this.stop(); else this.play();
            });
        }

        render(index, restart = true) {
            if (!this.items.length) return;
            this.index = (index + this.items.length) % this.items.length;
            const media = this.items[this.index];
            const old = q(this.inner, '.api-media-link');
            const link = createMediaLink(media, this.type);
            if (old) old.replaceWith(link); else this.loading.replaceWith(link);

            const image = media.bannerImage || media.coverImage?.extraLarge || media.coverImage?.large || '';
            this.bg.style.backgroundImage = image ? `url("${String(image).replaceAll('"', '%22')}")` : '';
            this.counter.textContent = `${this.index + 1} / ${this.items.length}`;
            this.root.classList.add('is-ready');
            if (restart) this.play(); else this.restartProgress();
        }

        restartProgress() {
            this.root.classList.remove('is-playing');
            void this.progress.offsetWidth;
            this.root.classList.add('is-playing');
        }

        go(delta) { this.render(this.index + delta); }

        stop() {
            clearInterval(this.timer);
            this.timer = null;
            this.root.classList.remove('is-playing');
        }

        play() {
            clearInterval(this.timer);
            this.restartProgress();
            this.timer = setInterval(() => this.render(this.index + 1, false), SLIDE_MS);
        }

        pauseVisual() {
            this.root.classList.add('is-paused');
            if (this.timer) { clearInterval(this.timer); this.timer = null; }
        }

        resumeVisual() {
            this.root.classList.remove('is-paused');
            this.play();
        }
    }

    function showFeedError(root, title, message) {
        root.classList.add('is-error');
        const loading = q(root, '.api-loading');
        if (loading) {
            loading.innerHTML = '';
            const strong = document.createElement('strong');
            strong.textContent = title;
            loading.append(strong, document.createTextNode(message));
        }
        const counter = q(root, '.api-counter');
        if (counter) counter.textContent = 'Offline';
    }

    async function initAniListFeeds() {
        const animeRoot = document.querySelector('[data-feed="anime"]');
        const mangaRoot = document.querySelector('[data-feed="manga"]');
        if (!animeRoot || !mangaRoot) return;

        try {
            const data = await fetchFeeds();
            const anime = normalizeAnime(data?.anime?.media);
            const manga = normalizeManga(data?.manga?.media);

            if (anime.length) new MediaSlider(animeRoot, anime, 'anime');
            else showFeedError(animeRoot, 'No airing titles found.', 'AniList returned no upcoming episodes right now.');

            if (manga.length) new MediaSlider(mangaRoot, manga, 'manga');
            else showFeedError(mangaRoot, 'No manga found.', 'AniList returned no trending releasing manga right now.');
        } catch (error) {
            console.warn('[Kirin Bio] AniList feed:', error);
            showFeedError(animeRoot, 'Anime feed unavailable.', 'The profile still works; this card will retry on the next page load.');
            showFeedError(mangaRoot, 'Manga feed unavailable.', 'The profile still works; this card will retry on the next page load.');
        }
    }

    function getVisitorSiteId() {
        const host = (window.location.hostname || '').toLowerCase().replace(/^www\./, '');
        return host || 'kirin-bio';
    }

    function formatVisitorCount(value) {
        const number = Number(value);
        if (!Number.isFinite(number) || number < 0) return '--';
        return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(number);
    }

    function mountVisitorTracker(siteId) {
        if (document.getElementById('kirinIcountTracker')) return;
        const mount = document.getElementById('kirinVisitorTrackerMount');
        if (!mount) return;

        const script = document.createElement('script');
        script.id = 'kirinIcountTracker';
        script.async = true;
        script.src = `https://icount.kr/c.js?id=${encodeURIComponent(siteId)}`;
        mount.appendChild(script);
    }

    async function refreshVisitorStats(siteId) {
        const card = document.getElementById('visitorLiveCard');
        const online = document.getElementById('visitorRealtime');
        const today = document.getElementById('visitorToday');
        const total = document.getElementById('visitorTotal');
        const updated = document.getElementById('visitorLiveUpdated');
        if (!card || !online || !today || !total || !updated) return;

        try {
            const response = await fetch(`https://icount.kr/api.php?id=${encodeURIComponent(siteId)}`, {
                method: 'GET',
                mode: 'cors',
                cache: 'no-store',
                credentials: 'omit'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            const data = await response.json();
            online.textContent = formatVisitorCount(data?.realtime);
            today.textContent = formatVisitorCount(data?.today?.uv ?? data?.today?.pv);
            total.textContent = formatVisitorCount(data?.total?.uv ?? data?.total?.pv);
            updated.textContent = 'Live · now';
            card.classList.remove('is-offline');
        } catch (error) {
            console.warn('[Kirin Bio] Realtime visitor:', error);
            updated.textContent = 'Visitor service offline';
            card.classList.add('is-offline');
        }
    }

    function initRealtimeVisitors() {
        const card = document.getElementById('visitorLiveCard');
        if (!card) return;

        const siteId = getVisitorSiteId();
        mountVisitorTracker(siteId);
        refreshVisitorStats(siteId);

        window.setInterval(() => {
            if (document.visibilityState === 'visible') refreshVisitorStats(siteId);
        }, VISITOR_REFRESH_MS);

        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') refreshVisitorStats(siteId);
        });
    }

    function initSocialPagination() {
        const bar = document.querySelector('.social-bento-bar');
        const pager = document.querySelector('.social-pagination');
        if (!bar || !pager) return;

        const items = Array.from(bar.querySelectorAll('.social-mini-item'));
        const prev = pager.querySelector('.social-page-prev');
        const next = pager.querySelector('.social-page-next');
        const indicator = pager.querySelector('.social-page-indicator');
        const perPage = 7;
        const totalPages = Math.max(1, Math.ceil(items.length / perPage));
        let currentPage = 0;
        let touchStartX = 0;

        function render(animate = true) {
            const start = currentPage * perPage;
            const end = start + perPage;
            items.forEach((item, index) => {
                item.hidden = index < start || index >= end;
                item.setAttribute('aria-hidden', item.hidden ? 'true' : 'false');
                if (item.hidden) item.setAttribute('tabindex', '-1');
                else item.removeAttribute('tabindex');
            });

            if (indicator) indicator.textContent = `${currentPage + 1} / ${totalPages}`;
            if (prev) prev.disabled = currentPage === 0;
            if (next) next.disabled = currentPage >= totalPages - 1;
            pager.classList.toggle('is-single-page', totalPages === 1);

            if (animate && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                bar.classList.remove('is-changing');
                void bar.offsetWidth;
                bar.classList.add('is-changing');
                window.setTimeout(() => bar.classList.remove('is-changing'), 260);
            }
        }

        function goTo(page) {
            const target = Math.max(0, Math.min(totalPages - 1, page));
            if (target === currentPage) return;
            currentPage = target;
            render(true);
        }

        prev?.addEventListener('click', () => goTo(currentPage - 1));
        next?.addEventListener('click', () => goTo(currentPage + 1));

        bar.addEventListener('touchstart', event => {
            touchStartX = event.changedTouches[0]?.clientX || 0;
        }, { passive: true });
        bar.addEventListener('touchend', event => {
            if (totalPages <= 1) return;
            const touchEndX = event.changedTouches[0]?.clientX || 0;
            const delta = touchEndX - touchStartX;
            if (Math.abs(delta) < 48) return;
            goTo(delta < 0 ? currentPage + 1 : currentPage - 1);
        }, { passive: true });

        render(false);
    }

    function initWebPagination() {
        const grid = document.querySelector('.web-bento-grid');
        const pager = document.querySelector('.web-pagination');
        if (!grid || !pager) return;

        const items = Array.from(grid.querySelectorAll(':scope > .bento-item'));
        const prev = pager.querySelector('.web-page-prev');
        const next = pager.querySelector('.web-page-next');
        const indicator = pager.querySelector('.web-page-indicator');
        let currentPage = 0;
        let touchStartX = 0;
        let lastPerPage = 0;

        const getPerPage = () => window.matchMedia('(max-width: 520px)').matches ? 4 : 5;

        function render(animate = true, keepFirstVisible = false) {
            const perPage = getPerPage();
            if (keepFirstVisible && lastPerPage && lastPerPage !== perPage) {
                const firstVisibleIndex = currentPage * lastPerPage;
                currentPage = Math.floor(firstVisibleIndex / perPage);
            }
            lastPerPage = perPage;

            const totalPages = Math.max(1, Math.ceil(items.length / perPage));
            currentPage = Math.max(0, Math.min(totalPages - 1, currentPage));
            const start = currentPage * perPage;
            const end = start + perPage;

            items.forEach((item, index) => {
                item.hidden = index < start || index >= end;
                item.setAttribute('aria-hidden', item.hidden ? 'true' : 'false');
                if (item.hidden) item.setAttribute('tabindex', '-1');
                else item.removeAttribute('tabindex');
            });

            if (indicator) indicator.textContent = `${currentPage + 1} / ${totalPages}`;
            if (prev) prev.disabled = currentPage === 0;
            if (next) next.disabled = currentPage >= totalPages - 1;
            pager.classList.toggle('is-single-page', totalPages === 1);

            if (animate && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                grid.classList.remove('is-changing');
                void grid.offsetWidth;
                grid.classList.add('is-changing');
                window.setTimeout(() => grid.classList.remove('is-changing'), 260);
            }
        }

        function goTo(page) {
            const perPage = getPerPage();
            const totalPages = Math.max(1, Math.ceil(items.length / perPage));
            const target = Math.max(0, Math.min(totalPages - 1, page));
            if (target === currentPage) return;
            currentPage = target;
            render(true);
        }

        prev?.addEventListener('click', () => goTo(currentPage - 1));
        next?.addEventListener('click', () => goTo(currentPage + 1));

        grid.addEventListener('touchstart', event => {
            touchStartX = event.changedTouches[0]?.clientX || 0;
        }, { passive: true });
        grid.addEventListener('touchend', event => {
            const perPage = getPerPage();
            const totalPages = Math.max(1, Math.ceil(items.length / perPage));
            if (totalPages <= 1) return;
            const touchEndX = event.changedTouches[0]?.clientX || 0;
            const delta = touchEndX - touchStartX;
            if (Math.abs(delta) < 48) return;
            goTo(delta < 0 ? currentPage + 1 : currentPage - 1);
        }, { passive: true });

        let resizeTimer = 0;
        window.addEventListener('resize', () => {
            window.clearTimeout(resizeTimer);
            resizeTimer = window.setTimeout(() => render(false, true), 120);
        }, { passive: true });

        render(false);
    }

    window.addEventListener('DOMContentLoaded', () => {
        initSocialPagination();
        initWebPagination();
        initRealtimeVisitors();
        const yearElement = document.getElementById('year');
        if (yearElement) yearElement.textContent = new Date().getFullYear();
        bindMediaModal();
        initAniListFeeds();
    });
})();
