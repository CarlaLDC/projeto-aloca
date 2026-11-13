import { HttpErrorResponse } from '@angular/common/http';
import { classeStatus, dataBr, diaRelativo, hCurta, hm, mensagemErro, somarDias } from './formato';

describe('formato', () => {
  it('formats times and dates', () => {
    expect(hm('09:30:00')).toBe('09:30');
    expect(hCurta('09:00:00')).toBe('09h');
    expect(hCurta('09:30:00')).toBe('09h30');
    expect(dataBr('2026-10-22')).toBe('22/10/2026');
    expect(somarDias('2026-12-31', 1)).toBe('2027-01-01');
  });

  it('writes relative days', () => {
    expect(diaRelativo('2026-10-22T09:00:00', '2026-10-22')).toBe('Hoje · 09:00');
    expect(diaRelativo('2026-10-21T14:00:00', '2026-10-22')).toBe('Ontem · 14:00');
    expect(diaRelativo('2026-10-25T08:00:00', '2026-10-22')).toBe('25/10 · 08:00');
  });

  it('maps statuses to CSS classes', () => {
    expect(classeStatus('APROVADA')).toBe('approved');
    expect(classeStatus('SOLICITADA')).toBe('requested');
    expect(classeStatus('REJEITADA')).toBe('cancelled');
  });

  it('extracts the API message from errors', () => {
    expect(mensagemErro(new HttpErrorResponse({ status: 400, error: { mensagem: 'Informe a atividade.' } }))).toBe('Informe a atividade.');
    expect(mensagemErro(new HttpErrorResponse({ status: 0 }))).toContain('servidor');
    expect(mensagemErro(new Error('x'), 'padrão')).toBe('padrão');
  });
});
