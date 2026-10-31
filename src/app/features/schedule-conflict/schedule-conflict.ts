import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-schedule-conflict',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './schedule-conflict.html',
  styleUrl: './schedule-conflict.css'
})
export class ScheduleConflict {
  readonly conflict = {
    resource: 'Laboratório de informática 02',
    location: 'Bloco C · 1º andar',
    date: 'Hoje, 30 de outubro de 2026',
    requestedTime: '09:00 – 11:00',
    conflictingTime: '10:00 – 12:00',
    activity: 'Aula de Programação Web',
    responsible: 'Mariana Costa'
  };

  readonly alternatives = [
    { day: 'Hoje', date: '30 out', time: '13:00 – 15:00', resource: 'Laboratório de informática 02', status: 'Disponível' },
    { day: 'Segunda-feira', date: '02 nov', time: '09:00 – 11:00', resource: 'Laboratório de informática 02', status: 'Disponível' },
    { day: 'Hoje', date: '30 out', time: '09:00 – 11:00', resource: 'Laboratório de informática 03', status: 'Disponível' }
  ];
}
