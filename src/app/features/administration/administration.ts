import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
interface Resource { id:number; name:string; type:string; location:string; capacity:number; status:'Ativo'|'Inativo'|'Em manutenção'; }
@Component({selector:'app-administration',standalone:true,imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./administration.html',styleUrl:'./administration.css'})
export class Administration {
 readonly tab=signal<'Recursos'|'Usuários'|'Bloqueios'>('Recursos'); readonly query=signal(''); readonly dialog=signal(false); readonly message=signal(''); readonly editingId=signal<number|null>(null);
 readonly resources=signal<Resource[]>([{id:1,name:'Laboratório 02',type:'Laboratório',location:'Bloco B · 2º andar',capacity:30,status:'Ativo'},{id:2,name:'Sala 204',type:'Sala de aula',location:'Bloco A · 2º andar',capacity:40,status:'Ativo'},{id:3,name:'Auditório',type:'Auditório',location:'Bloco C · Térreo',capacity:120,status:'Ativo'},{id:4,name:'Laboratório 01',type:'Laboratório',location:'Bloco B · 1º andar',capacity:25,status:'Em manutenção'}]);
 readonly users=signal([{id:1,name:'Ana Martins',email:'ana.martins@aloca.edu.br',role:'Administrador',status:'Ativo'},{id:2,name:'Ricardo Alves',email:'ricardo.alves@aloca.edu.br',role:'Responsável',status:'Ativo'},{id:3,name:'Mariana Costa',email:'mariana.costa@aloca.edu.br',role:'Professor',status:'Ativo'},{id:4,name:'Lucas Ferreira',email:'lucas.ferreira@aloca.edu.br',role:'Solicitante',status:'Inativo'}]);
 readonly blocks=signal([{id:1,resource:'Laboratório 01',reason:'Manutenção dos computadores',start:'2026-10-28',end:'2026-10-31'},{id:2,resource:'Projetor 03',reason:'Revisão técnica',start:'2026-11-03',end:'2026-11-04'}]);
 readonly activeResourcesCount=computed(()=>this.resources().filter(x=>x.status==='Ativo').length);
 readonly activeUsersCount=computed(()=>this.users().filter(x=>x.status==='Ativo').length);
 form={name:'',type:'Sala de aula',location:'',capacity:30};
 filteredResources(){const q=this.query().toLowerCase();return this.resources().filter(x=>[x.name,x.type,x.location,x.status].some(v=>v.toLowerCase().includes(q)));}
 filteredUsers(){const q=this.query().toLowerCase();return this.users().filter(x=>[x.name,x.email,x.role,x.status].some(v=>v.toLowerCase().includes(q)));}
 setTab(v:'Recursos'|'Usuários'|'Bloqueios'){this.tab.set(v);this.query.set('');this.message.set('');}
 openNew(){this.editingId.set(null);this.form={name:'',type:'Sala de aula',location:'',capacity:30};this.dialog.set(true);}
 edit(x:Resource){this.editingId.set(x.id);this.form={name:x.name,type:x.type,location:x.location,capacity:x.capacity};this.dialog.set(true);}
 save(){if(!this.form.name.trim()||!this.form.location.trim()||this.form.capacity<1)return;const id=this.editingId();if(id!==null){this.resources.update(xs=>xs.map(x=>x.id===id?{...x,name:this.form.name.trim(),type:this.form.type,location:this.form.location.trim(),capacity:Number(this.form.capacity)}:x));this.message.set('Recurso atualizado nesta demonstração.');}else{const nextId=Math.max(0,...this.resources().map(x=>x.id))+1;this.resources.update(xs=>[...xs,{id:nextId,name:this.form.name.trim(),type:this.form.type,location:this.form.location.trim(),capacity:Number(this.form.capacity),status:'Ativo'}]);this.message.set('Recurso cadastrado nesta demonstração.');}this.dialog.set(false);}
 toggleResource(x:Resource){this.resources.update(xs=>xs.map(y=>y.id===x.id?{...y,status:y.status==='Inativo'?'Ativo':'Inativo'}:y));this.message.set('Status do recurso atualizado nesta demonstração.');}
 toggleUser(x:{id:number;status:string}){this.users.update(xs=>xs.map(y=>y.id===x.id?{...y,status:y.status==='Ativo'?'Inativo':'Ativo'}:y));this.message.set('Acesso do usuário atualizado nesta demonstração.');}
 removeBlock(x:{id:number}){this.blocks.update(xs=>xs.filter(y=>y.id!==x.id));this.message.set('Bloqueio removido nesta demonstração.');}
}