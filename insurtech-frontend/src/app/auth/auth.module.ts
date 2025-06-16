import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { LoginComponent } from './login/login.component';
import { AuthGuard } from './auth.guard';

@NgModule({
  declarations: [LoginComponent],
  imports: [CommonModule, FormsModule, RouterModule],
  providers: [AuthGuard]
})
export class AuthModule {}
