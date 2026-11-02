import {expect, test} from '@playwright/test';
import {beregnOppsummering, tolkMedlemmer, tolkNomSok, type Gruppe} from '../../main/typescript/gruppelogikk';

const gruppe = (navn: string, ...navIdenter: string[]): Gruppe => ({
    navn,
    sokenavn: navn,
    medlemmer: navIdenter.map(navIdent => ({navIdent, visningNavn: navIdent}))
});

test('beregnOppsummering teller kun-i-første og fellesmedlemmer', () => {
    const resultat = beregnOppsummering(gruppe('A', '1', '2', '3'), [gruppe('B', '2'), gruppe('C', '3', '4')]);
    expect(resultat).toEqual([
        {grupper: 'Kun i A', antall: 1},
        {grupper: 'A og B', antall: 1},
        {grupper: 'A og C', antall: 1}
    ]);
});

test('tolkNomSok skiller tilgangsenhet, nom-enhet og ugyldig', () => {
    expect(tolkNomSok('1234')).toEqual({type: 'tilgangsenhet', verdi: '1234'});
    expect(tolkNomSok('ab123c')).toEqual({type: 'nom-enhet', verdi: 'ab123c'});
    expect(tolkNomSok('xyz')).toBeUndefined();
});

test('tolkMedlemmer krever liste og gyldige medlemmer', () => {
    const nom = {type: 'nom-enhet', verdi: 'ab123c'} as const;
    expect(tolkMedlemmer(nom, {ansatte: [{navident: 'A1', visningsnavn: 'A'}], navn: ' Enhet '}))
        .toEqual({medlemmer: [{navIdent: 'A1', visningNavn: 'A'}], enhetsnavn: 'Enhet'});
    expect(() => tolkMedlemmer(nom, [])).toThrow('forventet en liste');
    expect(() => tolkMedlemmer({type: 'gruppe', verdi: 'g'}, [{navIdent: 'A1'}])).toThrow('mangler nav-ident');
});
