import { Injectable } from '@angular/core';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';
import { catchError, map, tap } from 'rxjs/operators';


const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class CategoriesService {

  private catgegoriesUrl = 'http://inventory-amazin-dev.apps.ocp.datr.eu/products/types';

  constructor(private http: HttpClient) { }

  getCategories(): Observable<String[]> {
    const url = `${this.catgegoriesUrl}`;
    return this.http.get<String[]>(url)
    .pipe(
      tap(products => this.log(`fetched all categories`)),
      catchError(this.handleError<String[]>(`getCategories`))
    );
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T> (operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {

      // TODO: send the error to remote logging infrastructure
      console.error(error); // log to console instead

      // TODO: better job of transforming error for user consumption
      this.log(`${operation} failed: ${error.message}`);

      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }

  /** Log a HeroService message with the MessageService */
  private log(message: string) {
    console.log('CategoriesService: ' + message);
  }
}