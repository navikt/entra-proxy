export function hentElement<T extends Element>(
    selector: string,
    type: {new(): T},
    root: ParentNode = document
): T {
    const element = root.querySelector(selector);
    if (!(element instanceof type)) {
        throw new Error(`Mangler forventet element: ${selector}`);
    }
    return element;
}

export function feilmelding(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}

// Async-lyttere håndterer egne feil; dette gjør at DOM-en ikke får en Promise tilbake
export function trygg<E extends Event>(lytter: (hendelse: E) => Promise<void>): (hendelse: E) => void {
    return hendelse => {
        void lytter(hendelse);
    };
}
