(function () {
  if (document.documentElement.hasAttribute('data-atf-screenshot-viewer-bound')) return;
  document.documentElement.setAttribute('data-atf-screenshot-viewer-bound', 'true');
  const selector = 'a[data-atf-screenshot="base64"],a[data-atf-screenshot-ref]';
  function payload(anchor) {
    let source = anchor;
    if (!/^data:image\/png;base64,/.test(source.getAttribute('href') || ''))
      source = document.getElementById('atf-screenshot-' + anchor.getAttribute('data-atf-screenshot-ref'));
    const value = source && source.getAttribute('href');
    const isPng = /^data:image\/png;base64,/.test(value || '');
    return isPng ? value : null;
  }
  function readerPosition(anchor) {
    const positions = [], offset = anchor.getBoundingClientRect().top;
    for (let node = anchor.parentElement; node; node = node.parentElement)
      if (node.scrollHeight > node.clientHeight || node.scrollWidth > node.clientWidth)
        positions.push([node, node.scrollLeft, node.scrollTop]);
    return () => {
      positions.forEach(([node, left, top]) => { node.scrollLeft = left; node.scrollTop = top; });
      if (!anchor.isConnected) return;
      anchor.focus({ preventScroll: true });
      requestAnimationFrame(() => {
        if (!anchor.isConnected || document.activeElement !== anchor) return;
        const scroll = positions.find(([node]) => node.scrollHeight > node.clientHeight);
        if (scroll) scroll[0].scrollTop += anchor.getBoundingClientRect().top - offset;
        const rect = anchor.getBoundingClientRect();
        if (rect.bottom <= 0 || rect.top >= innerHeight)
          anchor.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'instant' });
      });
    };
  }
  function button(label, action) {
    const result = document.createElement('button');
    result.type = 'button'; result.textContent = label;
    result.setAttribute('aria-label', label); result.addEventListener('click', action);
    return result;
  }
  function open(anchor, uri) {
    if (document.querySelector('.atf-screenshot-viewer[open]')) return;
    const restore = readerPosition(anchor), dialog = document.createElement('dialog');
    dialog.className = 'atf-screenshot-viewer featherlight';
    dialog.setAttribute('aria-label', 'Screenshot viewer');
    dialog.setAttribute('data-view-mode', 'fit');
    const toolbar = document.createElement('header'), caption = document.createElement('span');
    caption.className = 'atf-screenshot-caption';
    caption.textContent = (anchor.getAttribute('aria-label') || 'Screenshot').replace(/^Open\s+/, '');
    caption.title = caption.textContent;
    const stage = document.createElement('div'), image = document.createElement('img');
    stage.className = 'atf-screenshot-stage'; image.className = 'featherlight-image';
    image.alt = caption.textContent;
    function fit() {
      if (!image.naturalWidth || !image.naturalHeight) return;
      const original = dialog.getAttribute('data-view-mode') === 'original';
      const style = getComputedStyle(stage);
      const width = stage.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
      const height = stage.clientHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom);
      const scale = original ? 1 : Math.min(width / image.naturalWidth, height / image.naturalHeight);
      image.style.width = image.naturalWidth * scale + 'px';
      image.style.height = image.naturalHeight * scale + 'px';
    }
    const mode = button('Original size', () => {
      const original = dialog.getAttribute('data-view-mode') !== 'original';
      dialog.setAttribute('data-view-mode', original ? 'original' : 'fit');
      mode.textContent = original ? 'Fit to screen' : 'Original size';
      mode.setAttribute('aria-label', mode.textContent);
      stage.scrollTop = 0; stage.scrollLeft = 0; fit();
    });
    const close = button('Close screenshot', () => dialog.close());
    close.className = 'featherlight-close-icon';
    toolbar.append(caption, mode, close); stage.append(image); dialog.append(toolbar, stage);
    const resize = new ResizeObserver(fit); resize.observe(stage);
    dialog.addEventListener('close', () => {
      resize.disconnect(); dialog.remove(); image.removeAttribute('src'); restore();
    }, { once: true });
    dialog.addEventListener('keydown', event => {
      if (event.key !== 'Tab') return;
      if (event.shiftKey && document.activeElement === mode) { event.preventDefault(); close.focus(); }
      else if (!event.shiftKey && document.activeElement === close) { event.preventDefault(); mode.focus(); }
    });
    dialog.addEventListener('wheel', event => {
      if (dialog.getAttribute('data-view-mode') === 'fit') event.preventDefault();
    }, { passive: false });
    image.addEventListener('load', fit);
    image.addEventListener('error', () => {
      const error = document.createElement('p'); error.setAttribute('role', 'status');
      error.textContent = 'The captured image could not be decoded.'; stage.replaceChildren(error);
    }, { once: true });
    document.body.append(dialog); image.src = uri; dialog.showModal(); close.focus(); fit();
  }
  function activate(event) {
    const anchor = event.target.closest && event.target.closest(selector);
    if (!anchor) return;
    const uri = payload(anchor); if (!uri) return;
    event.preventDefault(); event.stopImmediatePropagation(); open(anchor, uri);
  }
  document.addEventListener('click', event => {
    if (event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey) activate(event);
  }, true);
  document.addEventListener('keydown', event => {
    if (event.key === ' ' && !event.ctrlKey && !event.metaKey && !event.altKey) activate(event);
  }, true);
})();
