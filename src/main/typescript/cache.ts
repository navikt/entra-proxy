import {z} from 'zod';
import {feilmelding, hentElement, trygg} from './dom.js';
import {hentResponse, lesValidert} from './http.js';

(() => {
    const knapp = hentElement('#oppfrisk-caches', HTMLButtonElement);
    const status = hentElement('#cache-status', HTMLParagraphElement);

    knapp.addEventListener('click', trygg(async () => {
        knapp.disabled = true;
        status.hidden = false;
        status.classList.remove('error');
        status.textContent = 'Oppfrisker cache...';

        try {
            const response = await hentResponse('/api/v1/cache/flush', 'Kunne ikke oppfriske cache', {
                method: 'POST',
                redirect: 'error'
            });
            const antallSlettet: number = await lesValidert(response, z.number(), 'Uventet respons: forventet et antall.');
            status.textContent = `Cache oppfrisket. ${antallSlettet} nøkler slettet.`;
        } catch (error) {
            status.textContent = `Oppfrisking feilet: ${feilmelding(error)}`;
            status.classList.add('error');
        } finally {
            knapp.disabled = false;
        }
    }));
})();
