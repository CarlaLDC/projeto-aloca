import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

type ResourceType = 'Todos' | 'Salas' | 'Laboratórios' | 'Equipamentos';

interface Resource {
  id: number;
  name: string;
  type: Exclude<ResourceType, 'Todos'>;
  location: string;
  capacity: number;
  availableFrom: string;
  availableUntil: string;
  status: 'Disponível' | 'Ocupado';
}

@Component({
  selector: 'app-availability',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './availability.html',
  styleUrl: './availability.css'
})
export class Availability {
  readonly searchTerm = signal('');
  readonly selectedType = signal<ResourceType>('Todos');
  readonly selectedDate = signal(this.toDateInputValue(new Date()));
  readonly selectedView = signal<'lista' | 'horarios'>('lista');

  readonly types: ResourceType[] = ['Todos', 'Salas', 'Laboratórios', 'Equipamentos'];

  // Dados demonstrativos até a integração com a API de disponibilidade.
  readonly resources: Resource[] = [
    { id: 1, name: 'Sala 204', type: 'Salas', location: 'Bloco A · 2º andar', capacity: 35, availableFrom: '08:00', availableUntil: '18:00', status: 'Disponível' },
    { id: 2, name: 'Sala 108', type: 'Salas', location: 'Bloco B · 1º andar', capacity: 24, availableFrom: '13:00', availableUntil: '21:00', status: 'Disponível' },
    { id: 3, name: 'Laboratório de informática 02', type: 'Laboratórios', location: 'Bloco C · 1º andar', capacity: 30, availableFrom: '08:00', availableUntil: '12:00', status: 'Disponível' },
    { id: 4, name: 'Laboratório de química', type: 'Laboratórios', location: 'Bloco C · 2º andar', capacity: 20, availableFrom: '—', availableUntil: '—', status: 'Ocupado' },
    { id: 5, name: 'Projetor multimídia 04', type: 'Equipamentos', location: 'Central de materiais', capacity: 1, availableFrom: '09:00', availableUntil: '17:00', status: 'Disponível' }
  ];

  readonly filteredResources = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('pt-BR');
    return this.resources.filter(resource => {
      const matchesType = this.selectedType() === 'Todos' || resource.type === this.selectedType();
      const matchesTerm = !term || `${resource.name} ${resource.location}`.toLocaleLowerCase('pt-BR').includes(term);
      return matchesType && matchesTerm;
    });
  });

  readonly availableCount = computed(() => this.resources.filter(resource => resource.status === 'Disponível').length);
  readonly occupiedCount = computed(() => this.resources.length - this.availableCount());

  setType(type: ResourceType): void {
    this.selectedType.set(type);
  }

  changeDate(days: number): void {
    const date = new Date(`${this.selectedDate()}T12:00:00`);
    date.setDate(date.getDate() + days);
    this.selectedDate.set(this.toDateInputValue(date));
  }

  get formattedDate(): string {
    return new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
      .format(new Date(`${this.selectedDate()}T12:00:00`));
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
