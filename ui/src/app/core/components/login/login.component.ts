import { Component, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

import { InfoService, LoginService } from '@core/services';

@Component({
  selector: 'chutney-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnDestroy {

  username: string;
  password: string;
  connectionError: string;
  action: string;

  private forwardUrl: string;
  private paramsSubscription: Subscription;
  private queryParamsSubscription: Subscription;
  version = '';
  applicationName = '';

  constructor(
    private loginService: LoginService,
    private infoService: InfoService,
    private route: ActivatedRoute,
  ) {
    this.paramsSubscription = this.route.params.subscribe(params => {
      this.action = params['action'];
    });
    this.queryParamsSubscription = this.route.queryParams.subscribe(params => {
      this.forwardUrl = params['url'];
    });
    this.infoService.getVersion().subscribe(result => {
      this.version = result;
    });
    this.infoService.getApplicationName().subscribe(result => {
      this.applicationName = result;
    });
  }

  ngOnDestroy() {
    if (this.paramsSubscription) {
        this.paramsSubscription.unsubscribe();
    }
    if (this.queryParamsSubscription) {
        this.queryParamsSubscription.unsubscribe();
    }
  }

  login() {
    this.loginService.login(this.username, this.password)
      .subscribe(
        user => this.loginService.navigateAfterLogin(this.forwardUrl),
        error => {
            this.connectionError = error.error.message;
            this.action = null;
        }
      );
  }
}
