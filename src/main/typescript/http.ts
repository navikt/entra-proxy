import {z} from 'zod';

const problemSkjema = z.object({detail: z.string().nullish()});

// Uvalidert tolkning: bruk bare når kallerstedet selv sjekker formen på svaret
export async function lesJson<T>(response: Response): Promise<T> {
    return await response.json() as T;
}

export async function lesValidert<T extends z.ZodType>(response: Response, skjema: T, feilmelding: string): Promise<z.infer<T>> {
    const resultat = skjema.safeParse(await response.json());
    if (!resultat.success) {
        throw new Error(feilmelding);
    }
    return resultat.data;
}

export async function feilFraRespons(response: Response, standardmelding: string): Promise<Error> {
    if (response.headers.get('content-type')?.includes('json')) {
        const problem = problemSkjema.safeParse(await response.json());
        return new Error((problem.success ? problem.data.detail : undefined) ?? standardmelding);
    }
    return new Error(standardmelding);
}

export async function hentResponse(url: string, feilprefiks: string, innstillinger: RequestInit = {}): Promise<Response> {
    const headers = new Headers(innstillinger.headers);
    if (!headers.has('Accept')) headers.set('Accept', 'application/json');
    const response = await fetch(url, {...innstillinger, headers});
    if (!response.ok) {
        throw await feilFraRespons(response, `${feilprefiks} (HTTP ${response.status}).`);
    }
    return response;
}

export async function hentJson<T>(url: string, feilprefiks: string): Promise<T> {
    const response = await hentResponse(url, feilprefiks);
    return await lesJson<T>(response);
}

export async function hentTekst(url: string, feilprefiks: string): Promise<string> {
    const response = await fetch(url, {headers: {'Accept': 'text/plain'}});
    if (!response.ok) {
        throw new Error(`${feilprefiks} (HTTP ${response.status}).`);
    }
    return (await response.text()).trim();
}
