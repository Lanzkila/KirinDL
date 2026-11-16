(() => {
  'use strict';

  const API = 'https://api.github.com/repos/Lanzkila/KirinDL';
  const CACHE_TTL = 60 * 60 * 1000;
  const MAX_RELEASE_PAGES = 25;
  const numberFormat = new Intl.NumberFormat('en-US');

  const version = document.getElementById('latest-version');
  const latestLink = document.getElementById('latest-link');
  const downloads = document.getElementById('github-downloads');
  const forks = document.getElementById('github-forks');
  const stars = document.getElementById('github-stars');
  const year = document.getElementById('year');

  if (year) year.textContent = new Date().getFullYear();

  function initLoading() {
    const loader = document.getElementById('page-loader');
    const message = document.getElementById('loader-message');
    if (!loader) return;

    let alreadyEntered = false;
    try {
      alreadyEntered = sessionStorage.getItem('kirindl-pages-entered') === '1';
      sessionStorage.setItem('kirindl-pages-entered', '1');
    } catch (_) {
      // The first-visit splash still works when session storage is unavailable.
    }

    function show(text) {
      if (message && text) message.textContent = text;
      loader.classList.remove('is-hidden');
      loader.setAttribute('aria-hidden', 'false');
    }

    function hide() {
      loader.classList.add('is-hidden');
      loader.setAttribute('aria-hidden', 'true');
    }

    if (alreadyEntered) {
      hide();
    } else {
      const startedAt = Date.now();
      const finishFirstVisit = () => {
        const elapsed = Date.now() - startedAt;
        window.setTimeout(hide, Math.max(220, 620 - elapsed));
      };
      if (document.readyState === 'complete') finishFirstVisit();
      else window.addEventListener('load', finishFirstVisit, { once: true });
      window.setTimeout(hide, 8000);
    }

    document.addEventListener('click', event => {
      if (event.defaultPrevented || event.button !== 0 ||
          event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;

      const target = event.target instanceof Element ? event.target : event.target.parentElement;
      if (!target) return;
      if (target.closest('[data-no-loading]')) return;

      const anchor = target.closest('a[href]');
      if (anchor && !anchor.hasAttribute('download')) {
        const rawHref = anchor.getAttribute('href') || '';
        if (!rawHref || rawHref.startsWith('javascript:')) return;

        let destination;
        try { destination = new URL(anchor.href, window.location.href); }
        catch (_) { return; }

        show('Opening destination…');

        const samePageHash = destination.origin === window.location.origin &&
          destination.pathname === window.location.pathname &&
          destination.search === window.location.search && destination.hash;

        if (samePageHash) {
          window.setTimeout(hide, 420);
          return;
        }

        const opensNewContext = anchor.target === '_blank' ||
          event.ctrlKey || event.metaKey || event.shiftKey || event.button === 1;

        if (destination.origin === window.location.origin && !opensNewContext) {
          event.preventDefault();
          window.setTimeout(() => window.location.assign(destination.href), 180);
          return;
        }

        window.setTimeout(hide, 850);
        return;
      }

      const control = target.closest('button, [role="button"], summary, [data-loading]');
      if (!control || control.disabled || control.closest('.page-loader')) return;
      show('Loading…');
      window.setTimeout(hide, 420);
    });
  }

  function rememberLastPage() {
    const storageKey = 'kirindl-pages:last-page:v1';
    const pageLinks = Array.from(document.querySelectorAll('[data-page-link]'));
    const resume = document.querySelector('[data-menu-resume]');
    const resumeLink = document.querySelector('[data-last-page-link]');
    const resumeName = document.querySelector('[data-last-page-name]');

    function pathOf(url) {
      try {
        return new URL(url, window.location.href).pathname.replace(/\/+$/, '') || '/';
      } catch (_) {
        return '';
      }
    }

    function save(link) {
      if (!link) return;
      const href = link.getAttribute('href');
      const record = {
        path: pathOf(link.href),
        label: link.getAttribute('data-page-label') || link.textContent.trim(),
        href: href
      };
      if (!record.path || !record.href) return;
      try {
        window.localStorage.setItem(storageKey, JSON.stringify(record));
      } catch (_) {
        // Page navigation continues normally when storage is unavailable.
      }
    }

    pageLinks.forEach(link => {
      link.addEventListener('click', () => save(link));
    });

    const currentPage = pageLinks.find(link =>
      pathOf(link.href) === pathOf(window.location.pathname)
    );
    if (currentPage) {
      currentPage.setAttribute('aria-current', 'page');
      save(currentPage);
    }

    let saved = null;
    try {
      saved = JSON.parse(window.localStorage.getItem(storageKey) || 'null');
    } catch (_) {
      saved = null;
    }

    const matchingPage = saved && pageLinks.find(link =>
      pathOf(link.href) === saved.path
    );

    const viewingSavedPage = Boolean(matchingPage) &&
      pathOf(window.location.pathname) === saved.path;

    if (resume && resumeLink && resumeName && matchingPage && !viewingSavedPage) {
      resumeLink.href = matchingPage.getAttribute('href');
      resumeName.textContent = 'Continue: ' + (matchingPage.getAttribute('data-page-label') || 'Last page');
      resume.hidden = false;
    } else if (resume) {
      resume.hidden = true;
    }
  }

  function initUniversalMenu() {
    const menuTrigger = document.querySelector('[data-menu-toggle]');
    const menu = document.querySelector('[data-site-menu]');
    if (!menuTrigger || !menu) return;

    function setOpen(open) {
      menu.hidden = !open;
      menuTrigger.setAttribute('aria-expanded', String(open));
      menuTrigger.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
    }

    menuTrigger.addEventListener('click', () => {
      setOpen(menuTrigger.getAttribute('aria-expanded') !== 'true');
    });

    document.addEventListener('click', event => {
      if (!menu.contains(event.target) && !menuTrigger.contains(event.target)) {
        setOpen(false);
      }
    });

    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') setOpen(false);
    });

    // Any details[data-menu-submenu] added later joins this shared accordion behavior.
    const groups = Array.from(menu.querySelectorAll('details[data-menu-submenu]'));
    groups.forEach(group => {
      group.addEventListener('toggle', () => {
        if (!group.open) return;
        groups.forEach(other => {
          if (other !== group) other.open = false;
        });
      });
    });
  }

  rememberLastPage();
  initUniversalMenu();

  const rail = document.getElementById('project-cards');
  const previous = document.getElementById('cards-prev');
  const next = document.getElementById('cards-next');

  function updateRailButtons() {
    if (!rail) return;

    const maxScroll = Math.max(0, rail.scrollWidth - rail.clientWidth);
    const atFirstCard = rail.scrollLeft <= 2;
    const atLastCard = maxScroll <= 2 || rail.scrollLeft >= maxScroll - 2;

    if (previous) {
      previous.disabled = atFirstCard;
      previous.setAttribute('aria-disabled', String(atFirstCard));
    }
    if (next) {
      next.disabled = atLastCard;
      next.setAttribute('aria-disabled', String(atLastCard));
    }
  }

  function scrollOneCard(direction) {
    if (!rail) return;
    const firstCard = rail.querySelector('.project-card');
    if (!firstCard) return;

    const styles = window.getComputedStyle(rail);
    const gap = parseFloat(styles.columnGap || styles.gap) || 14;
    const distance = firstCard.getBoundingClientRect().width + gap;
    rail.scrollBy({ left: direction * distance, behavior: 'smooth' });
  }

  if (rail) {
    if (previous) previous.addEventListener('click', () => scrollOneCard(-1));
    if (next) next.addEventListener('click', () => scrollOneCard(1));

    let railFrame = 0;
    const scheduleRailUpdate = () => {
      if (railFrame) window.cancelAnimationFrame(railFrame);
      railFrame = window.requestAnimationFrame(() => {
        railFrame = 0;
        updateRailButtons();
      });
    };

    rail.addEventListener('scroll', scheduleRailUpdate, { passive: true });
    rail.addEventListener('scrollend', updateRailButtons);
    window.addEventListener('resize', scheduleRailUpdate);

    if ('ResizeObserver' in window) {
      const railObserver = new ResizeObserver(scheduleRailUpdate);
      railObserver.observe(rail);
    }

    window.setTimeout(updateRailButtons, 100);
  }

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

  initLoading();
  void loadRepositoryStats();
  void loadReleaseStats();
})();