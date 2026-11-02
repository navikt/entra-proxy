import type {Ansatt} from './api-types.js';

export function nullstillSortering(overskrift: ParentNode): void {
    for (const knapp of overskrift.querySelectorAll<HTMLButtonElement>('.sorter-knapp')) {
        knapp.dataset.sortering = '';
        knapp.closest('th')?.removeAttribute('aria-sort');
    }
}

export function lagSorterbarOverskrift(
    tekst: string,
    kolonneIndeks: number,
    overskrift: ParentNode,
    rader: HTMLTableSectionElement
): HTMLTableCellElement {
    const celle = document.createElement('th');
    celle.scope = 'col';
    const knapp = document.createElement('button');
    knapp.className = 'sorter-knapp';
    knapp.type = 'button';
    knapp.textContent = tekst;
    knapp.addEventListener('click', () => {
        const retning = knapp.dataset.sortering === 'ascending' ? 'descending' : 'ascending';
        nullstillSortering(overskrift);
        knapp.dataset.sortering = retning;
        celle.setAttribute('aria-sort', retning);
        const faktor = retning === 'ascending' ? 1 : -1;
        const sorterte = [...rader.rows].sort((radA, radB) =>
            faktor * radA.cells[kolonneIndeks].textContent.trim()
                .localeCompare(radB.cells[kolonneIndeks].textContent.trim(), undefined, {numeric: true, sensitivity: 'base'})
        );
        rader.append(...sorterte);
    });
    celle.appendChild(knapp);
    return celle;
}

export function lagMedlemRad(medlem: Ansatt, ...ekstraceller: HTMLTableCellElement[]): HTMLTableRowElement {
    const navIdentCelle = document.createElement('th');
    navIdentCelle.scope = 'row';
    const lenke = document.createElement('a');
    lenke.className = 'navident-lenke';
    lenke.href = `/ansatt.html?ansattid=${encodeURIComponent(medlem.navIdent)}`;
    lenke.textContent = medlem.navIdent;
    navIdentCelle.appendChild(lenke);
    const rad = document.createElement('tr');
    rad.append(navIdentCelle, lagCelle(medlem.visningNavn ?? ''), ...ekstraceller);
    return rad;
}

export function lagCelle(tekst = ''): HTMLTableCellElement {
    const celle = document.createElement('td');
    celle.textContent = tekst;
    return celle;
}
