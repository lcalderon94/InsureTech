import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { LoginRequest, TokenResponse } from '../auth.service';

@Component({
  selector: 'app-login',
  template: `
    <h2>Login</h2>
    <form (ngSubmit)="login()">
      <input name="username" [(ngModel)]="username" placeholder="Username" />
      <input name="password" type="password" [(ngModel)]="password" placeholder="Password" />
      <button type="submit">Login</button>
    </form>
    <div *ngIf="error" class="error">{{ error }}</div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  error: string | null = null;

  constructor(private router: Router, private authService: AuthService) {}

  login() {
    this.error = null;
    const creds: LoginRequest = { username: this.username, password: this.password };
    this.authService.login(creds).subscribe({
      next: (res: TokenResponse) => {
        localStorage.setItem('token', res.accessToken);
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.error = 'Credenciales inválidas';
      }
    });
  }
}
