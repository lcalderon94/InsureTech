import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { LoginComponent } from './login/login.component';
import { AuthGuard } from './auth.guard';
import { AuthService } from './auth.service';

@NgModule({
  declarations: [LoginComponent],
  imports: [CommonModule, FormsModule, RouterModule, HttpClientModule],
  providers: [AuthGuard, AuthService]
})
export class AuthModule {}
