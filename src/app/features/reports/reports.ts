import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface ResourceReport {
  name: string;
  usage: number;
  hours: number;
  reservations: number;
  conflicts: number;
}

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class Reports {
  readonly resources: ResourceReport[] = [
    { name: 'Laboratório 02', usage: 84, hours: 120, reservations: 42, conflicts: 9 },
    { name: 'Sala 204', usage: 72, hours: 96, reservations: 36, conflicts: 6 },
    { name: 'Auditório', usage: 56, hours: 68, reservations: 18, conflicts: 3 }
  ];

  exportReport(): void {
    const rows = [
      ['Recurso', 'Utilização (%)', 'Horas alocadas', 'Reservas', 'Conflitos evitados'],
      ...this.resources.map(resource => [
        resource.name,
        String(resource.usage),
        String(resource.hours),
        String(resource.reservations),
        String(resource.conflicts)
      ])
    ];
    const csv = rows.map(row => row.map(value => '"' + value.replaceAll('"', '""') + '"').join(';')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'relatorio-aloca.csv';
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
