import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PolicyService, Policy } from './policy.service';

@Component({
  selector: 'app-policy-detail',
  template: `
    <div *ngIf="policy">
      <h3>Policy {{ policy.policyNumber }}</h3>
      <pre>{{ policy | json }}</pre>
    </div>
  `
})
export class PolicyDetailComponent implements OnInit {
  policy?: Policy;

  constructor(private route: ActivatedRoute, private policyService: PolicyService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.policyService.getPolicy(+id).subscribe(p => (this.policy = p));
    }
  }
}
