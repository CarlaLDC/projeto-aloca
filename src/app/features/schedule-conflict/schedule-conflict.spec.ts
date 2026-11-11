import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConflitoStore } from '../../core/conflito.store';
import { ScheduleConflict } from './schedule-conflict';

describe('ScheduleConflict', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScheduleConflict],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('formats the conflict and preserves reservation data in edit parameters', () => {
    TestBed.inject(ConflitoStore).ultimo.set({
      mensagem: 'Este horário não está disponível: sala ocupada.',
      atividade: 'Aula de programação', data: '2030-10-22', inicio: '09:00:00', fim: '11:00:00',
      espacoId: 5, professorId: 2, materialIds: [8],
      conflitos: [{
        tipo: 'Sala', recurso: 'Sala 204', localizacao: 'Bloco A', inicio: '10:00:00', fim: '12:00:00',
        atividade: 'Outra aula', responsavel: 'Roberto Almeida', sobreposicaoMinutos: 60
      }],
      alternativas: []
    });

    const component = TestBed.createComponent(ScheduleConflict).componentInstance;
    expect(component.conflict()?.resource).toBe('Sala 204');
    expect(component.motivo()).toBe('Sala ocupada.');
    expect(component.sobreposicao()).toBe('1 hora');
    expect(component.editarParams()).toEqual({
      atividade: 'Aula de programação', data: '2030-10-22', inicio: '09:00', fim: '11:00',
      espaco: '5', professor: '2', material: '8'
    });
  });

  it('has no conflict data when opened directly', () => {
    const component = TestBed.createComponent(ScheduleConflict).componentInstance;
    expect(component.conflict()).toBeNull();
    expect(component.barras()).toBeNull();
    expect(component.editarParams()).toEqual({});
  });
});