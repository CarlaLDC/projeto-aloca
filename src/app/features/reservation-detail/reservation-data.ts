export type ReservationStatus = 'Aprovada' | 'Solicitada' | 'Concluída' | 'Cancelada';
export interface AuditEvent {
  date: string;
  actor: string;
  action: string;
  description: string;
}
export interface DemoReservation {
  id: string;
  resource: string;
  schedule: string;
  status: ReservationStatus;
  statusClass: string;
  activity: string;
  room: string;
  date: string;
  start: string;
  end: string;
  location: string;
  teacher: string;
  material: string;
  requester: string;
  history: AuditEvent[];
}

// Fixtures compartilhadas entre a lista e o detalhe; não representam dados do servidor.
export const DEMO_RESERVATIONS: DemoReservation[] = [
  {
    id: 'RES-0248', resource: 'Laboratório 02', schedule: '22/10 · 09h–11h',
    status: 'Aprovada', statusClass: 'approved', activity: 'Aula de Programação Web',
    room: 'Laboratório de informática 02', date: '2026-10-22', start: '09:00', end: '11:00',
    location: 'Bloco B', teacher: 'Mariana Costa', material: 'Projetor 04', requester: 'Ana Martins',
    history: [
      { date: '21/10 · 16:42', actor: 'Ana Martins', action: 'Solicitada', description: 'Reserva criada' },
      { date: '21/10 · 17:05', actor: 'Paulo Souza', action: 'Aprovada', description: 'Alocação validada' },
      { date: '21/10 · 17:05', actor: 'Sistema', action: 'Notificação', description: 'Enviada · simulação' }
    ]
  },
  {
    id: 'RES-0249', resource: 'Projetor 04', schedule: '22/10 · 14h–16h',
    status: 'Solicitada', statusClass: 'requested', activity: 'Reserva de Projetor 04',
    room: 'Projetor 04', date: '2026-10-22', start: '14:00', end: '16:00', location: 'Não informado',
    teacher: 'Não informado', material: 'Projetor 04', requester: 'Ana Martins', history: []
  },
  {
    id: 'RES-0241', resource: 'Sala 204', schedule: '21/10 · 19h–21h',
    status: 'Concluída', statusClass: 'completed', activity: 'Reserva de Sala 204',
    room: 'Sala 204', date: '2026-10-21', start: '19:00', end: '21:00', location: 'Não informado',
    teacher: 'Não informado', material: 'Não informado', requester: 'Ana Martins', history: []
  },
  {
    id: 'RES-0238', resource: 'Auditório', schedule: '20/10 · 10h–12h',
    status: 'Cancelada', statusClass: 'cancelled', activity: 'Reserva de Auditório',
    room: 'Auditório', date: '2026-10-20', start: '10:00', end: '12:00', location: 'Não informado',
    teacher: 'Não informado', material: 'Não informado', requester: 'Ana Martins', history: []
  }
];
