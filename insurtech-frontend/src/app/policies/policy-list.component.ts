import { Component, OnInit } from '@angular/core';
import { PolicyService, Policy } from './policy.service';

@Component({
  selector: 'app-policy-list',
  template: `
    <h3>Policies</h3>
    <ul>
      <li *ngFor="let policy of policies">
        <a [routerLink]="[policy.id]">{{ policy.policyNumber }}</a>
      </li>
    </ul>
  `
})
export class PolicyListComponent implements OnInit {
  policies: Policy[] = [];

  constructor(private policyService: PolicyService) {}

  ngOnInit(): void {
    this.policyService.getPolicies().subscribe(p => (this.policies = p));
  }
}
