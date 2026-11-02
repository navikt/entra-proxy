import {hentElement} from './dom.js';

(() => {
    const arsak = new URLSearchParams(window.location.search).get("arsak");
    if (arsak) {
        hentElement('#arsak-verdi', HTMLElement).textContent = arsak;
        hentElement('#arsak', HTMLParagraphElement).hidden = false;
    }
})();
