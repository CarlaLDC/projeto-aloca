import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ConflitoStore } from '../../core/conflito.store';
import { ConflitoResposta, OpcaoProfessor, OpcaoRecurso, ReservaDetalhe } from '../../core/api.models';
import { dataBr, hojeIso, mensagemErro } from '../../core/formato';

@Component({
  selector: 'app-new-reservation',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './new-reservation.html',
  styleUrl: './new-reservation.css'
})
export class NewReservation implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly conflitos = inject(ConflitoStore);
  private readonly params = inject(ActivatedRoute).snapshot.queryParamMap;
  readonly auth = inject(AuthService);

  readonly reviewing = signal(false);
  readonly completed = signal(false);
  readonly sending = signal(false);
  readonly error = signal('');
  readonly created = signal<ReservaDetalhe | null>(null);
  readonly rooms = signal<OpcaoRecurso[]>([]);
  readonly teachers = signal<OpcaoProfessor[]>([]);
  readonly materials = signal<OpcaoRecurso[]>([]);
  readonly today = hojeIso();
  readonly dataBr = dataBr;

  activity = this.params.get('atividade') ?? '';
  date = this.params.get('data') ?? this.today;
  start = this.params.get('inicio') ?? '09:00';
  end = this.params.get('fim') ?? '';
  room: number | null = this.numero('espaco');
  teacher: number | null = this.numero('professor');
  material: number | null = this.numero('material');

  ngOnInit(): void {
    if (!this.end) this.end = this.maisDuasHoras(this.start);
    this.api.opcoesReserva().subscribe({
      next: opcoes => {
        this.rooms.set(opcoes.espacos);
        this.teachers.set(opcoes.professores);
        this.materials.set(opcoes.materiais);
        if (this.room === null || !opcoes.espacos.some(r => r.id === this.room)) this.room = opcoes.espacos[0]?.id ?? null;
      },
      error: e => this.error.set(mensagemErro(e, 'Não foi possível carregar salas, professores e materiais.'))
    });
  }

  get selectedRoom(): OpcaoRecurso | undefined { return this.rooms().find(r => r.id === Number(this.room)); }
  get selectedTeacher(): OpcaoProfessor | undefined { return this.teachers().find(t => t.id === Number(this.teacher)); }
  get selectedMaterial(): OpcaoRecurso | undefined { return this.materials().find(m => m.id === Number(this.material)); }

  /** Sala ou material restrito exige aprovação do Responsável (RF-07). */
  get restricted(): boolean {
    return !!this.selectedRoom?.restrito || !!this.selectedMaterial?.restrito;
  }

  get restrictedNames(): string {
    return [this.selectedRoom, this.selectedMaterial].filter(r => r?.restrito).map(r => r!.nome).join(' e ');
  }

  review(form: NgForm): void {
    this.error.set('');
    if (form.invalid || !this.activity.trim() || this.room === null) {
      form.control.markAllAsTouched();
      this.error.set('Preencha todos os campos obrigatórios.');
      return;
    }
    if (this.date < this.today) {
      this.error.set('Escolha a data de hoje ou uma data futura.');
      return;
    }
    if (this.end <= this.start) {
      this.error.set('O término deve ser posterior ao início.');
      return;
    }
    this.reviewing.set(true);
  }

  confirm(): void {
    if (this.sending() || this.room === null) return;
    this.sending.set(true);
    this.error.set('');
    this.api.criarReserva({
      atividade: this.activity.trim(),
      data: this.date,
      inicio: this.start,
      fim: this.end,
      espacoId: Number(this.room),
      professorId: this.teacher === null ? null : Number(this.teacher),
      materialIds: this.material === null ? [] : [Number(this.material)]
    }).subscribe({
      next: reserva => {
        this.sending.set(false);
        this.created.set(reserva);
        this.completed.set(true);
        this.auth.atualizarNotificacoes();
      },
      error: (e: unknown) => {
        this.sending.set(false);
        if (e instanceof HttpErrorResponse && e.status === 409 && (e.error as ConflitoResposta)?.conflitos) {
          this.conflitos.ultimo.set(e.error as ConflitoResposta);
          void this.router.navigate(['/conflito-horario']);
          return;
        }
        this.reviewing.set(false);
        this.error.set(mensagemErro(e));
      }
    });
  }

  private numero(chave: string): number | null {
    const valor = this.params.get(chave);
    return valor && !Number.isNaN(Number(valor)) ? Number(valor) : null;
  }

  private maisDuasHoras(hora: string): string {
    const [h, m] = hora.split(':').map(Number);
    return `${String(Math.min(h + 2, 22)).padStart(2, '0')}:${String(h + 2 > 22 ? 0 : m).padStart(2, '0')}`;
  }
}
