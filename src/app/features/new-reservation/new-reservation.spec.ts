import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NewReservation } from './new-reservation';

describe('NewReservation', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [NewReservation, FormsModule], providers: [provideRouter([])] }).compileComponents();
  });

  async function render() {
    const fixture = TestBed.createComponent(NewReservation);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  it('rejects an end time before the start time', async () => {
    const fixture = await render();
    fixture.componentInstance.end = '08:00';
    fixture.detectChanges();
    await fixture.whenStable();
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.reviewing()).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('posterior');
  });

  it('preserves values when returning from review to editing', async () => {
    const fixture = await render();
    fixture.componentInstance.activity = 'Projeto integrador';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.reviewing()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Projeto integrador');
    fixture.nativeElement.querySelector('.secondary').click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[name="activity"]').value).toBe('Projeto integrador');
  });

  it('shows the approval notice only for the restricted room', async () => {
    const fixture = await render();
    expect(fixture.nativeElement.querySelector('.notice')).not.toBeNull();
    fixture.componentInstance.room = fixture.componentInstance.rooms[1];
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.notice')).toBeNull();
  });
});
