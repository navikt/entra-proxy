import {feilmelding, hentElement} from './dom.js';
import {feilFraRespons} from './http.js';

(() => {
    const knapp = hentElement('#oppfrisk-caches', HTMLButtonElement);
    const status = hentElement('#cache-status', HTMLParagraphElement);

    knapp.addEventListener('click', async () => {
        knapp.disabled = true;
        status.hidden = false;
        status.classList.remove('error');
        status.textContent = 'Oppfrisker cache...';

        try {
            const response = await fetch('/api/v1/cache/flush', {
                method: 'POST',
                headers: {'Accept': 'application/json'},
                redirect: 'error'
            });
            if (!response.ok) {
                throw await feilFraRespons(response, `Kunne ikke oppfriske cache (HTTP ${response.status}).`);
            }
            const antallSlettet: number = await response.json();
            status.textContent = `Cache oppfrisket. ${antallSlettet} nøkler slettet.`;
        } catch (error) {
            status.textContent = `Oppfrisking feilet: ${feilmelding(error)}`;
            status.classList.add('error');
        } finally {
            knapp.disabled = false;
        }
    });
})();
