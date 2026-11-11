import { Injectable, signal } from '@angular/core';
import { ConflitoResposta } from './api.models';

/** Guarda o último conflito devolvido pela API para a tela "Conflito de horário" exibir. */
@Injectable({ providedIn: 'root' })
export class ConflitoStore {
  readonly ultimo = signal<ConflitoResposta | null>(null);
}
