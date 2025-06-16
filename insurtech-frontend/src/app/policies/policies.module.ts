import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CommonModule } from '@angular/common';

const routes: Routes = [
  { path: '', component: DummyPoliciesComponent }
];

import { Component } from '@angular/core';
@Component({
  template: '<h3>Policies works!</h3>'
})
export class DummyPoliciesComponent {}

@NgModule({
  declarations: [DummyPoliciesComponent],
  imports: [CommonModule, RouterModule.forChild(routes)]
})
export class PoliciesModule {}
