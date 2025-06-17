import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

import { PolicyListComponent } from './policy-list.component';
import { PolicyDetailComponent } from './policy-detail.component';

const routes: Routes = [
  { path: '', component: PolicyListComponent },
  { path: ':id', component: PolicyDetailComponent }
];

@NgModule({
  declarations: [PolicyListComponent, PolicyDetailComponent],
  imports: [CommonModule, HttpClientModule, RouterModule.forChild(routes)]
})
export class PoliciesModule {}
