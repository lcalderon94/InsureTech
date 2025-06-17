import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Policy {
  id: number;
  policyNumber: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private baseUrl = `${environment.apiUrl}/api/policies`;

  constructor(private http: HttpClient) {}

  getPolicies(): Observable<Policy[]> {
    return this.http.get<Policy[]>(this.baseUrl);
  }

  getPolicy(id: number): Observable<Policy> {
    return this.http.get<Policy>(`${this.baseUrl}/${id}`);
  }
}
