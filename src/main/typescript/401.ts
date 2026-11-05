import {hentElement} from './dom.js';

(() => {
    const navident = new URLSearchParams(window.location.search).get("navident");
    if (navident) {
        hentElement('#navident-verdi', HTMLElement).textContent = navident;
        hentElement('#navident', HTMLParagraphElement).hidden = false;
    }
})();
