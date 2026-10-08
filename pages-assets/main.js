(() => {
  const version = document.getElementById('latest-version');
  const link = document.getElementById('latest-link');
  document.getElementById('year').textContent = new Date().getFullYear();

  fetch('https://api.github.com/repos/Lanzkila/KirinDL/releases?per_page=20')
    .then(r => r.ok ? r.json() : Promise.reject(new Error('release request failed')))
    .then(items => {
      const release = items.find(x => !x.draft) || items[0];
      if (!release) throw new Error('no release');
      version.textContent = release.tag_name || release.name || 'Available';
      if (release.html_url) link.href = release.html_url;
    })
    .catch(() => { version.textContent = 'See Releases'; });
})();