import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  template: `
    <h2>Login</h2>
    <form (ngSubmit)="login()">
      <input name="username" [(ngModel)]="username" placeholder="Username" />
      <input name="password" type="password" [(ngModel)]="password" placeholder="Password" />
      <button type="submit">Login</button>
    </form>
  `
})
export class LoginComponent {
  username = '';
  password = '';

  constructor(private router: Router) {}

  login() {
    // TODO: connect to auth service
    if (this.username && this.password) {
      localStorage.setItem('token', 'dummy');
      this.router.navigateByUrl('/');
    }
  }
}
