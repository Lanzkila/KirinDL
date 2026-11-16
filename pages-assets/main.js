(() => {
  'use strict';

  const API = 'https://api.github.com/repos/Lanzkila/KirinDL';
  const CACHE_TTL = 60 * 60 * 1000;
  const MAX_RELEASE_PAGES = 25;
  const numberFormat = new Intl.NumberFormat('en-US');

  const version = document.getElementById('latest-version');
  const latestCard = document.getElementById('latest-release-card');
  const prereleaseVersion = document.getElementById('prerelease-version');
  const prereleaseCard = document.getElementById('prerelease-release-card');
  const downloads = document.getElementById('github-downloads');
  const forks = document.getElementById('github-forks');
  const stars = document.getElementById('github-stars');
  const year = document.getElementById('year');

  if (year) year.textContent = new Date().getFullYear();

  function initSplashCanvas(loader) {
    const canvas = loader.querySelector('[data-splash-canvas]');
    if (!canvas) return { start() {}, stop() {} };

    let ctx = null;
    try { ctx = canvas.getContext('2d', { alpha: true }); }
    catch (_) { ctx = null; }

    if (!ctx) {
      const fallback = document.createElement('div');
      fallback.className = 'splash-virus-fallback';
      fallback.setAttribute('aria-hidden', 'true');
      for (let i = 0; i < 22; i++) {
        const particle = document.createElement('span');
        particle.style.setProperty('--x', (Math.random() * 100).toFixed(2) + '%');
        particle.style.setProperty('--y', (Math.random() * 100).toFixed(2) + '%');
        particle.style.setProperty('--dx', (Math.random() * 180 - 90).toFixed(0) + 'px');
        particle.style.setProperty('--dy', (Math.random() * 180 - 90).toFixed(0) + 'px');
        particle.style.setProperty('--duration', (7 + Math.random() * 9).toFixed(2) + 's');
        particle.style.setProperty('--delay', (-Math.random() * 12).toFixed(2) + 's');
        fallback.appendChild(particle);
      }
      loader.appendChild(fallback);
      return { start() {}, stop() {} };
    }

    const glyphs = ['{ }', '</>', '01', '[]', '=>', ';', '//', '&&', '0x', '< >'];
    const particles = [];
    let bonds = [];
    let width = 0;
    let height = 0;
    let pixelRatio = 1;
    let frameId = 0;
    let running = false;
    let lastTime = 0;
    let lastResize = 0;
    const reducedMotion = window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function randomVelocity() {
      const speed = .12 + Math.random() * .32;
      return (Math.random() < .5 ? -1 : 1) * speed;
    }

    function makeParticle() {
      return {
        x: Math.random() * Math.max(1, width),
        y: Math.random() * Math.max(1, height),
        vx: randomVelocity(),
        vy: randomVelocity(),
        alpha: .18 + Math.random() * .36,
        size: 10 + Math.random() * 5,
        glyph: glyphs[Math.floor(Math.random() * glyphs.length)],
        nextTurn: performance.now() + 500 + Math.random() * 1800
      };
    }

    function resize() {
      const bounds = loader.getBoundingClientRect();
      width = Math.max(1, bounds.width);
      height = Math.max(1, bounds.height);
      pixelRatio = Math.min(1.75, window.devicePixelRatio || 1);
      canvas.width = Math.round(width * pixelRatio);
      canvas.height = Math.round(height * pixelRatio);
      ctx.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);

      const desired = Math.max(26, Math.min(64, Math.round(width * height / 24000)));
      while (particles.length < desired) particles.push(makeParticle());
      while (particles.length > desired) particles.pop();
      draw(performance.now());
    }

    function draw(now) {
      ctx.clearRect(0, 0, width, height);

      // Draw a faint code-grid texture beneath the drifting glyph particles.
      ctx.save();
      ctx.strokeStyle = 'rgba(138,216,255,.035)';
      ctx.lineWidth = 1;
      const step = 54;
      for (let x = step; x < width; x += step) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, height); ctx.stroke();
      }
      for (let y = step; y < height; y += step) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(width, y); ctx.stroke();
      }
      ctx.restore();

      // A collision creates a short-lived bond. The line holds briefly, then breaks.
      const active = [];
      for (const bond of bonds) {
        if (now < bond.until) {
          active.push(bond);
          const dx = bond.b.x - bond.a.x;
          const dy = bond.b.y - bond.a.y;
          const distance = Math.max(1, Math.hypot(dx, dy));
          const pull = .0025;
          bond.a.vx += dx / distance * pull;
          bond.a.vy += dy / distance * pull;
          bond.b.vx -= dx / distance * pull;
          bond.b.vy -= dy / distance * pull;
          ctx.beginPath();
          ctx.moveTo(bond.a.x, bond.a.y);
          ctx.lineTo(bond.b.x, bond.b.y);
          ctx.strokeStyle = 'rgba(138,216,255,' + (0.24 + .18 * (1 - (bond.until - now) / bond.duration)) + ')';
          ctx.lineWidth = 1;
          ctx.shadowColor = 'rgba(138,216,255,.42)';
          ctx.shadowBlur = 9;
          ctx.stroke();
          ctx.shadowBlur = 0;
        } else {
          const dx = bond.b.x - bond.a.x;
          const dy = bond.b.y - bond.a.y;
          const distance = Math.max(1, Math.hypot(dx, dy));
          const kick = .16;
          bond.a.vx -= dx / distance * kick;
          bond.a.vy -= dy / distance * kick;
          bond.b.vx += dx / distance * kick;
          bond.b.vy += dy / distance * kick;
        }
      }
      bonds = active;

      for (let i = 0; i < particles.length; i++) {
        const a = particles[i];
        if (now >= a.nextTurn) {
          a.vx = Math.max(-.62, Math.min(.62, a.vx + randomVelocity() * .55));
          a.vy = Math.max(-.62, Math.min(.62, a.vy + randomVelocity() * .55));
          a.nextTurn = now + 700 + Math.random() * 1900;
        }

        const hasBond = bonds.some(bond => bond.a === a || bond.b === a);
        const drift = hasBond ? .55 : 1;
        a.x += a.vx * drift;
        a.y += a.vy * drift;

        if (a.x < 14 || a.x > width - 14) a.vx *= -1;
        if (a.y < 14 || a.y > height - 14) a.vy *= -1;
        a.x = Math.max(8, Math.min(width - 8, a.x));
        a.y = Math.max(8, Math.min(height - 8, a.y));

        ctx.font = '500 ' + a.size + 'px ui-monospace, SFMono-Regular, Menlo, monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = 'rgba(138,216,255,' + a.alpha + ')';
        ctx.shadowColor = 'rgba(138,216,255,.32)';
        ctx.shadowBlur = 8;
        ctx.fillText(a.glyph, a.x, a.y);
        ctx.shadowBlur = 0;
      }

      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const a = particles[i], b = particles[j];
          const distance = Math.hypot(b.x - a.x, b.y - a.y);
          if (distance < 23 && !bonds.some(bond =>
              (bond.a === a && bond.b === b) || (bond.a === b && bond.b === a))) {
            const duration = 820 + Math.random() * 260;
            bonds.push({ a, b, duration, until: now + duration });
          }
        }
      }
    }

    function tick(now) {
      if (!running || document.hidden) {
        frameId = 0;
        return;
      }
      const delta = Math.min(2, (now - (lastTime || now)) / 16.67);
      lastTime = now;
      if (now - lastResize > 1000) {
        const bounds = loader.getBoundingClientRect();
        if (Math.abs(bounds.width - width) > 2 || Math.abs(bounds.height - height) > 2) resize();
        lastResize = now;
      }
      if (delta > 0) draw(now);
      frameId = window.requestAnimationFrame(tick);
    }

    function start() {
      if (running) return;
      running = true;
      lastTime = 0;
      resize();
      if (!reducedMotion) frameId = window.requestAnimationFrame(tick);
    }

    function stop() {
      running = false;
      if (frameId) window.cancelAnimationFrame(frameId);
      frameId = 0;
      lastTime = 0;
    }

    document.addEventListener('visibilitychange', () => {
      if (document.hidden) stop();
      else if (!loader.classList.contains('is-hidden')) start();
    });

    window.addEventListener('resize', () => {
      if (running) resize();
    });

    return { start, stop };
  }

  function initLoading() {
    const loader = document.getElementById('page-loader');
    const message = document.getElementById('loader-message');
    if (!loader) return;
    const splashEffects = initSplashCanvas(loader);

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
      splashEffects.start();
    }

    function hide() {
      loader.classList.add('is-hidden');
      loader.setAttribute('aria-hidden', 'true');
      splashEffects.stop();
    }

    if (alreadyEntered) {
      hide();
    } else {
      splashEffects.start();
      const startedAt = Date.now();
      const finishFirstVisit = () => {
        const elapsed = Date.now() - startedAt;
        window.setTimeout(hide, Math.max(220, 3200 - elapsed));
      };
      if (document.readyState === 'complete') finishFirstVisit();
      else window.addEventListener('load', finishFirstVisit, { once: true });
      window.setTimeout(hide, 15000);
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
          window.setTimeout(hide, 750);
          return;
        }

        const opensNewContext = anchor.target === '_blank' ||
          event.ctrlKey || event.metaKey || event.shiftKey || event.button === 1;

        if (destination.origin === window.location.origin && !opensNewContext) {
          event.preventDefault();
          window.setTimeout(() => window.location.assign(destination.href), 650);
          return;
        }

        window.setTimeout(hide, 1100);
        return;
      }

      const control = target.closest('button, [role="button"], summary, [data-loading]');
      if (!control || control.disabled || control.closest('.page-loader')) return;
      show('Loading…');
      window.setTimeout(hide, 750);
    });
  }

  // Account panel logic is intentionally parked; restore its markup and initializer when ready.
  function initAccountPanel() {
    const trigger = document.querySelector('[data-account-toggle]');
    const panel = document.querySelector('[data-account-menu]');
    const backdrop = document.querySelector('[data-account-backdrop]');
    if (!trigger || !panel) return;

    function setOpen(open) {
      panel.hidden = !open;
      if (backdrop) backdrop.hidden = !open;
      trigger.setAttribute('aria-expanded', String(open));
      document.body.classList.toggle('account-modal-open', open);
    }

    if (backdrop) {
      backdrop.addEventListener('click', () => setOpen(false));
    }

    trigger.addEventListener('click', () => {
      const open = trigger.getAttribute('aria-expanded') !== 'true';
      if (open) {
        const menu = document.querySelector('[data-site-menu]');
        const menuTrigger = document.querySelector('[data-menu-toggle]');
        if (menu) menu.hidden = true;
        if (menuTrigger) {
          menuTrigger.setAttribute('aria-expanded', 'false');
          menuTrigger.setAttribute('aria-label', 'Open menu');
        }
      }
      setOpen(open);
    });

    document.addEventListener('click', event => {
      if (!panel.contains(event.target) && !trigger.contains(event.target)) setOpen(false);
    });

    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        setOpen(false);
        trigger.focus();
      }
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
      const open = menuTrigger.getAttribute('aria-expanded') !== 'true';
      if (open) {
        const accountPanel = document.querySelector('[data-account-menu]');
        const accountTrigger = document.querySelector('[data-account-toggle]');
        if (accountPanel) accountPanel.hidden = true;
        if (accountTrigger) accountTrigger.setAttribute('aria-expanded', 'false');
      }
      setOpen(open);
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
  // Temporarily disabled: re-enable when the KirinDL account flow is ready.

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

  function showRelease(card, target, release, emptyLabel) {
    if (!card || !target) return;
    const label = release && (release.tag || release.name);
    target.textContent = label || emptyLabel;
    card.href = release && release.url &&
      release.url.startsWith('https://github.com/Lanzkila/KirinDL/releases/')
      ? release.url
      : 'https://github.com/Lanzkila/KirinDL/releases';
    card.setAttribute('aria-label', label
      ? 'View ' + label + ' on GitHub'
      : 'View KirinDL releases on GitHub');
  }

  function showReleases(stable, prerelease) {
    showRelease(latestCard, version, stable, 'See Releases');
    showRelease(prereleaseCard, prereleaseVersion, prerelease, 'No devpatch yet');
  }

  function isNewerRelease(candidate, current) {
    if (!current) return true;
    const candidateDate = Date.parse(candidate.publishedAt || '') ||
      Date.parse(candidate.createdAt || '') || 0;
    const currentDate = Date.parse(current.publishedAt || '') ||
      Date.parse(current.createdAt || '') || 0;
    return candidateDate > currentDate;
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
    const cacheKey = 'kirindl-pages-release-stats-v2';
    const cached = readCache(cacheKey);
    if (cached) {
      showCount(downloads, cached.downloads);
      showReleases(cached.stable, cached.preRelease);
      if (fresh(cached)) return;
    }

    try {
      let totalApkDownloads = 0;
      let latestStable = null;
      let latestDevpatch = null;
      let finished = false;

      for (let page = 1; page <= MAX_RELEASE_PAGES; page++) {
        const releases = await requestJson(API + '/releases?per_page=100&page=' + page);
        if (!Array.isArray(releases)) throw new Error('Invalid releases response');

        for (const release of releases) {
          if (!release || release.draft) continue;

          const record = {
            tag: release.tag_name || '',
            name: release.name || '',
            url: release.html_url || '',
            publishedAt: release.published_at || '',
            createdAt: release.created_at || ''
          };

          if (!release.prerelease && isNewerRelease(record, latestStable)) {
            latestStable = record;
          }
          if (release.prerelease && /devpatch/i.test(record.tag) &&
              isNewerRelease(record, latestDevpatch)) {
            latestDevpatch = record;
          }

          if (!Array.isArray(release.assets)) continue;
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
      showReleases(latestStable, latestDevpatch);
      saveCache(cacheKey, {
        downloads: totalApkDownloads,
        stable: latestStable,
        preRelease: latestDevpatch
      });
    } catch (error) {
      console.warn('KirinDL release statistics unavailable:', error);
      if (!cached) {
        if (downloads) downloads.textContent = '—';
        showReleases(null, null);
      }
    }
  }

  initLoading();
  void loadRepositoryStats();
  void loadReleaseStats();
})();