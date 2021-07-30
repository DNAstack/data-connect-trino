(window["webpackJsonp"] = window["webpackJsonp"] || []).push([["main"],{

/***/ "./src/$$_lazy_route_resource lazy recursive":
/*!**********************************************************!*\
  !*** ./src/$$_lazy_route_resource lazy namespace object ***!
  \**********************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

function webpackEmptyAsyncContext(req) {
	// Here Promise.resolve().then() is used instead of new Promise() to prevent
	// uncaught exception popping up in devtools
	return Promise.resolve().then(function() {
		var e = new Error("Cannot find module '" + req + "'");
		e.code = 'MODULE_NOT_FOUND';
		throw e;
	});
}
webpackEmptyAsyncContext.keys = function() { return []; };
webpackEmptyAsyncContext.resolve = webpackEmptyAsyncContext;
module.exports = webpackEmptyAsyncContext;
webpackEmptyAsyncContext.id = "./src/$$_lazy_route_resource lazy recursive";

/***/ }),

/***/ "./src/app/app-config.service.ts":
/*!***************************************!*\
  !*** ./src/app/app-config.service.ts ***!
  \***************************************/
/*! exports provided: AppConfigService */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppConfigService", function() { return AppConfigService; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_common_http__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/common/http */ "./node_modules/@angular/common/fesm5/http.js");
/* harmony import */ var _environments_environment__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ../environments/environment */ "./src/environments/environment.ts");




var AppConfigService = /** @class */ (function () {
    function AppConfigService(http) {
        this.http = http;
    }
    AppConfigService.prototype.loadAppConfig = function () {
        var _this = this;
        return this.http.get('/assets/appConfig.json')
            .toPromise()
            .then(function (data) {
            _this.config = Object.assign({}, _environments_environment__WEBPACK_IMPORTED_MODULE_3__["environment"], data);
            console.debug('Runtime configuration', _this.config);
        });
    };
    AppConfigService = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Injectable"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_angular_common_http__WEBPACK_IMPORTED_MODULE_2__["HttpClient"]])
    ], AppConfigService);
    return AppConfigService;
}());



/***/ }),

/***/ "./src/app/app-routing.module.ts":
/*!***************************************!*\
  !*** ./src/app/app-routing.module.ts ***!
  \***************************************/
/*! exports provided: AppRoutingModule */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppRoutingModule", function() { return AppRoutingModule; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_router__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/router */ "./node_modules/@angular/router/fesm5/router.js");



var routes = [];
var AppRoutingModule = /** @class */ (function () {
    function AppRoutingModule() {
    }
    AppRoutingModule = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["NgModule"])({
            imports: [_angular_router__WEBPACK_IMPORTED_MODULE_2__["RouterModule"].forRoot(routes)],
            exports: [_angular_router__WEBPACK_IMPORTED_MODULE_2__["RouterModule"]]
        })
    ], AppRoutingModule);
    return AppRoutingModule;
}());



/***/ }),

/***/ "./src/app/app.api.service.ts":
/*!************************************!*\
  !*** ./src/app/app.api.service.ts ***!
  \************************************/
/*! exports provided: ApiService */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "ApiService", function() { return ApiService; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_common_http__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/common/http */ "./node_modules/@angular/common/fesm5/http.js");
/* harmony import */ var _app_config_service__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./app-config.service */ "./src/app/app-config.service.ts");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var rxjs_operators__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! rxjs/operators */ "./node_modules/rxjs/_esm5/operators/index.js");






var ApiService = /** @class */ (function () {
    function ApiService(httpClient, app, snackBar) {
        this.httpClient = httpClient;
        this.app = app;
        this.snackBar = snackBar;
        this.wepKcUrl = 'https://wep-keycloak.staging.dnastack.com/auth/realms/DNAstack/protocol/openid-connect/token';
        this.apiUrl = app.config.apiUrl;
        this.wesUrl = app.config.wesUrl;
    }
    ApiService.prototype.getFields = function () {
        var _this = this;
        return this.httpClient.get(this.apiUrl + "/fields")
            .pipe(Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_5__["catchError"])(function (err) { return _this.errorSnack(err); }));
    };
    ApiService.prototype.doQuery = function (query) {
        var _this = this;
        return this.httpClient.post(this.apiUrl + "/search", query)
            .pipe(Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_5__["catchError"])(function (err) { return _this.errorSnack(err); }));
    };
    ApiService.prototype.doWorkflowExecution = function (token, params) {
        var inputsJsonFileContents = "{\"calculateMd5.input_file\": " + params.vcf_object + "}";
        var file = new File([inputsJsonFileContents], 'inputs.json', { type: 'application/json' });
        var wepWorkflowUrl = 'https://wep-restapi.staging.dnastack.com/api/workflow/organization/303276/project/303293/workflow/303424';
        var wesRunsUrl = 'https://wes-translator.staging.dnastack.com/ga4gh/wes/v1/runs';
        var headers = new _angular_common_http__WEBPACK_IMPORTED_MODULE_2__["HttpHeaders"]();
        headers = headers
            .append('Authorization', "Bearer " + token);
        var data = new FormData();
        data.append('workflow_url', wepWorkflowUrl);
        data.append('workflow_params', file);
        return this.httpClient.post(wesRunsUrl, data, { headers: headers });
    };
    ApiService.prototype.getToken = function (delayTime) {
        if (delayTime === void 0) { delayTime = 0; }
        var headers = new _angular_common_http__WEBPACK_IMPORTED_MODULE_2__["HttpHeaders"]();
        headers = headers.append("Content-Type", "application/x-www-form-urlencoded");
        var body = 'grant_type=password&client_id=dnastack-client&username=marc&password=changeit';
        return this.httpClient.post(this.wepKcUrl, body, { headers: headers }).pipe(Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_5__["delay"])(delayTime), Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_5__["map"])(function (_a) {
            var access_token = _a.access_token;
            return access_token;
        }));
    };
    ApiService.prototype.monitorJob = function (token, runId) {
        var headers = new _angular_common_http__WEBPACK_IMPORTED_MODULE_2__["HttpHeaders"]();
        headers = headers
            .append('Authorization', "Bearer " + token);
        return this.httpClient.get("https://wes-translator.staging.dnastack.com/ga4gh/wes/v1/runs/" + runId + "/status", { headers: headers });
    };
    ApiService.prototype.errorSnack = function (err) {
        this.snackBar.open(err.message, null, {
            panelClass: 'error-snack'
        });
        throw err;
    };
    ApiService = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Injectable"])({
            providedIn: 'root'
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_angular_common_http__WEBPACK_IMPORTED_MODULE_2__["HttpClient"],
            _app_config_service__WEBPACK_IMPORTED_MODULE_3__["AppConfigService"],
            _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatSnackBar"]])
    ], ApiService);
    return ApiService;
}());



/***/ }),

/***/ "./src/app/app.component.html":
/*!************************************!*\
  !*** ./src/app/app.component.html ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<div class=\"full-page\" style=\"display: flex; flex-direction: column\">\n  <mat-toolbar color=\"primary\" style=\"height:64px\">\n    <button matTooltip=\"Toggle Search\" class=\"m-r-sm\" mat-icon-button (click)=\"leftSidenav.toggle()\"><i\n        class=\"material-icons\">search</i></button>\n    <div>{{sitename}}</div>\n    <span class=\"fill-space\"></span>\n    <button matTooltip=\"Toggle Workflow Executor\" class=\"m-r-sm\" mat-icon-button (click)=\"rightSidenav.toggle()\"><i\n      class=\"material-icons\">cloud_queue</i></button>\n  </mat-toolbar>\n  <div style=\"flex:1\">\n    <mat-sidenav-container style=\"height: 100%\">\n      <mat-sidenav position=\"start\" #leftSidenav mode=\"side\" [(opened)]=\"view.leftSidebarOpened\" style=\"min-width:400px\">\n          <div class=\"display:hidden\">Stuff</div>\n            <mat-toolbar class=\"toolbar-white\" style=\"position: absolute; top:0; width: 100%\">\n              <div>Search</div>\n              <span class=\"fill-space\"></span>\n            </mat-toolbar>\n            <div style=\"position:absolute; left:0; right:0; top:64px; bottom:64px; overflow-y: auto\">\n              <mat-toolbar style=\"background: none\">\n                <mat-toolbar-row>\n                  <button tabindex=\"-1\" matTooltip=\"Show fields\" style=\"display:inline\" class=\"m-r-sm\" mat-stroked-button\n                    (click)=\"showFields()\"><i class=\"material-icons\">grid_on</i></button>\n                  <button [hidden]=\"!view.showJSONs\" tabindex=\"-1\" matTooltip=\"Show Query JSON\" style=\"display:inline\" class=\"m-r-sm\"\n                    mat-stroked-button (click)=\"showJson(transformQuery(), 'Query JSON')\"><i class=\"material-icons\">code</i></button>\n                  <span class=\"fill-space\"></span>\n                  <!--button matTooltip=\"Show starred results\" style=\"display:inline\" mat-stroked-button (click)=\"showCart($event)\"><i class=\"material-icons\">star</i></button-->\n                  <button tabindex=\"-1\" matTooltip=\"Search\" style=\"display:inline\" class=\"m-l-sm\" mat-stroked-button\n                    color=\"{{view.queryChanged ? 'primary' : '' }}\" (click)=\"doQuery(query)\"\n                    [disabled]=\"view.isQuerying || !view.queryChanged\"><i class=\"material-icons\"\n                      *ngIf=\"!view.isQuerying\">search</i>\n                    <div *ngIf=\"view.isQuerying\" style=\"width:32px; height:26px\">\n                      <mat-spinner style=\"margin-top:7px; margin-left:6px\" [diameter]=\"20\"></mat-spinner>\n                    </div>\n                  </button>\n                </mat-toolbar-row>\n              </mat-toolbar>\n              <div class=\"clearfix\"></div>\n              <mat-accordion>\n                <mat-expansion-panel [hidden]=\"!view.displayQueryComponents.select\">\n                  <mat-expansion-panel-header>\n                    <mat-panel-title>\n                      Select\n                    </mat-panel-title>\n                  </mat-expansion-panel-header>\n                  <div>\n                    <div class=\"m-b-sm\">\n                      <button tabindex=\"-1\" mat-button (click)=\"selectAllFields(true)\" class=\"m-r-sm\">All</button><button\n                        tabindex=\"-1\" (click)=\"selectAllFields(false)\" mat-button>None</button>\n                    </div>\n                    <div *ngFor=\"let field of config.fields | keyvalue\">\n                      <mat-checkbox [checked]=\"isFieldSelected(field.value)\"\n                        (change)=\"toggleFieldSelection($event,field.value)\">\n                          <div>{{field.value.name}}</div>\n                          <span class=\"text-muted\" style=\"font-style: italic\">{{field.value.table}}</span>\n                        </mat-checkbox>\n                    </div>\n                  </div>\n                </mat-expansion-panel>\n                <mat-expansion-panel [hidden]=\"!view.displayQueryComponents.from\">\n                  <mat-expansion-panel-header>\n                    <mat-panel-title>\n                      From\n                    </mat-panel-title>\n                  </mat-expansion-panel-header>\n                  <div>\n                    {{query.from}}\n                  </div>\n                  <div class=\"text-muted\">\n                    This value is hardcoded\n                  </div>\n                </mat-expansion-panel>\n                <mat-expansion-panel (opened)=\"panelOpenState = true\" (closed)=\"panelOpenState = false\" [hidden]=\"!view.displayQueryComponents.where\">\n                  <mat-expansion-panel-header>\n                    <mat-panel-title>\n                      Where\n                    </mat-panel-title>\n                  </mat-expansion-panel-header>\n                  <div>\n                    <query-builder [(ngModel)]=\"query.where\" (ngModelChange)=\"queryChanged($event)\" [config]=\"config\"\n                      *ngIf=\"config.fields\">\n\n                      <ng-container\n                        *queryButtonGroup=\"let ruleset; let addRule=addRule; let addRuleSet=addRuleSet; let removeRuleSet=removeRuleSet\">\n                        <button type=\"button\" matTooltip=\"Add Rule\" mat-icon-button color=\"primary\" (click)=\"addRule()\">\n                          <mat-icon>add</mat-icon>\n                        </button>\n                        <button type=\"button\" matTooltip=\"Add Ruleset\" mat-icon-button color=\"primary\" *ngIf=\"addRuleSet\"\n                          (click)=\"addRuleSet()\">\n                          <mat-icon>add_circle_outline</mat-icon>\n                        </button>\n                        <button type=\"button\" mat-icon-button matTooltip=\"Remove Ruleset\" color=\"accent\"\n                          *ngIf=\"removeRuleSet\" (click)=\"removeRuleSet()\">\n                          <mat-icon>remove_circle_outline</mat-icon>\n                        </button>\n                      </ng-container>\n\n                      <ng-container *queryArrowIcon>\n                        <mat-icon ngClass=\"mat-arrow-icon\">chevron_right</mat-icon>\n                      </ng-container>\n\n                      <ng-container *queryRemoveButton=\"let rule; let removeRule=removeRule\">\n                        <button type=\"button\" mat-icon-button matTooltip=\"Remove Rule\" color=\"accent\"\n                          (click)=\"removeRule(rule)\">\n                          <mat-icon>remove</mat-icon>\n                        </button>\n                      </ng-container>\n\n                      <ng-container *querySwitchGroup=\"let ruleset; let onChange=onChange\">\n                        <mat-radio-group *ngIf=\"ruleset\" [(ngModel)]=\"ruleset.condition\"\n                          (ngModelChange)=\"onChange($event)\">\n                          <mat-radio-button [style.padding.px]=\"10\" value=\"and\">And</mat-radio-button>\n                          <mat-radio-button [style.padding.px]=\"10\" value=\"or\">Or</mat-radio-button>\n                        </mat-radio-group>\n                      </ng-container>\n\n                      <ng-container\n                        *queryField=\"let rule; let fields=fields; let onChange=onChange; let getFields = getFields\">\n                        <mat-form-field>\n                          <mat-select [(ngModel)]=\"rule.field\" (ngModelChange)=\"onChange($event, rule)\">\n                            <mat-option *ngFor=\"let field of getFields(rule.entity)\" [value]=\"field.value\">\n                              {{ field.name }}\n                            </mat-option>\n                          </mat-select>\n                        </mat-form-field>\n                      </ng-container>\n\n                      <ng-container *queryOperator=\"let rule; let operators=operators; let onChange=onChange\">\n                        <mat-form-field [style.width.px]=\"90\">\n                          <mat-select [(ngModel)]=\"rule.operator\" (ngModelChange)=\"onChange()\">\n                            <mat-option *ngFor=\"let value of operators\" [value]=\"value\">\n                              {{ value }}\n                            </mat-option>\n                          </mat-select>\n                        </mat-form-field>\n                      </ng-container>\n\n                      <ng-container *queryInput=\"let rule; type: 'phenotype'\">\n                        <button mat-stroked-button>Choose HPO Term(s)</button>\n                      </ng-container>\n\n                      <ng-container *queryInput=\"let rule; type: 'boolean'; let onChange=onChange\">\n                        <mat-checkbox [(ngModel)]=\"rule.value\" (ngModelChange)=\"onChange()\"></mat-checkbox>\n                      </ng-container>\n\n                      <ng-container\n                        *queryInput=\"let rule; let field=field; let options=options; type: 'category'; let onChange=onChange\">\n                        <mat-form-field>\n                          <mat-select [(ngModel)]=\"rule.value\" (ngModelChange)=\"onChange()\">\n                            <mat-option *ngFor=\"let opt of options\" [value]=\"opt.value\">\n                              {{ opt.name }}\n                            </mat-option>\n                          </mat-select>\n                        </mat-form-field>\n                      </ng-container>\n\n                      <ng-container *queryInput=\"let rule; type: 'date'; let onChange=onChange\">\n                        <mat-form-field>\n                          <input matInput [matDatepicker]=\"picker\" [(ngModel)]=\"rule.value\" (ngModelChange)=\"onChange()\">\n                          <mat-datepicker-toggle matSuffix [for]=\"picker\"></mat-datepicker-toggle>\n                          <mat-datepicker #picker></mat-datepicker>\n                        </mat-form-field>\n                      </ng-container>\n\n                      <ng-container\n                        *queryInput=\"let rule; let options=options; type: 'multiselect'; let onChange=onChange\">\n                        <mat-form-field [style.width.px]=\"300\">\n                          <mat-select [(ngModel)]=\"rule.value\" multiple (ngModelChange)=\"onChange()\">\n                            <mat-option *ngFor=\"let opt of options\" [value]=\"opt.value\">\n                              {{ opt.name }}\n                            </mat-option>\n                          </mat-select>\n                        </mat-form-field>\n                      </ng-container>\n\n                      <ng-container *queryInput=\"let rule; let field=field; type: 'number'; let onChange=onChange\">\n                        <mat-form-field [style.width.px]=\"100\">\n                          <input matInput [(ngModel)]=\"rule.value\" type=\"number\" (ngModelChange)=\"onChange()\">\n                        </mat-form-field>\n                        {{field.units}}\n                      </ng-container>\n\n                      <!-- TODO: Improve -->\n                      <ng-container *queryInput=\"let rule; let field=field; type: 'json'; let onChange=onChange\">\n                        <mat-form-field>\n                          <input matInput [(ngModel)]=\"rule.value\" (ngModelChange)=\"onChange()\">\n                        </mat-form-field>\n                      </ng-container>\n\n                      <!-- TODO: Improve -->\n                      <ng-container *queryInput=\"let rule; let field=field; type: 'string'; let onChange=onChange\">\n                        <mat-form-field>\n                          <input matInput [(ngModel)]=\"rule.value\" (ngModelChange)=\"onChange()\">\n                        </mat-form-field>\n                      </ng-container>\n\n                      <ng-container *queryInput=\"let rule; let field=field; type: 'string[]'; let onChange=onChange\">\n                        <mat-form-field>\n                          <input matInput [(ngModel)]=\"rule.value\" (ngModelChange)=\"onChange()\">\n                        </mat-form-field>\n                      </ng-container>\n\n                      <ng-container *queryInput=\"let rule; let field=field; type: 'textarea'; let onChange=onChange\">\n                        <mat-form-field>\n                          <textarea matInput [(ngModel)]=\"rule.value\" (ngModelChange)=\"onChange()\">\n                            </textarea>\n                        </mat-form-field>\n                      </ng-container>\n\n                    </query-builder>\n                  </div>\n                </mat-expansion-panel>\n                <mat-expansion-panel [hidden]=\"!view.displayQueryComponents.limit\">\n                  <mat-expansion-panel-header>\n                    <mat-panel-title>\n                      Limit\n                    </mat-panel-title>\n                  </mat-expansion-panel-header>\n                  <div>\n                    {{query.limit}}\n                  </div>\n                  <div class=\"text-muted\">\n                    This value is changed using the paginator\n                  </div>\n                </mat-expansion-panel>\n                <mat-expansion-panel  [hidden]=\"!view.displayQueryComponents.offset\">\n                  <mat-expansion-panel-header>\n                    <mat-panel-title>\n                      Offset\n                    </mat-panel-title>\n                  </mat-expansion-panel-header>\n                  <div>\n                    {{query.offset}}\n                  </div>\n                  <div class=\"text-muted\">\n                    This value is changed using the paginator\n                  </div>\n                </mat-expansion-panel>\n              </mat-accordion>\n            </div>\n            <div class=\"b-t\" style=\"position: absolute; bottom:0; width: 100%; z-index: 100; background-color: white\">\n              <div class=\"m-a\"><a href=\"https://github.com/DNAstack/ga4gh-search\" target=\"_blank\"><span\n                    class=\"fa fa-fw fa-github\"></span>GitHub</a>\n                  </div>\n            </div>\n      </mat-sidenav>\n      <mat-sidenav position=\"end\" #rightSidenav mode=\"side\" [(opened)]=\"view.rightSidebarOpened\" style=\"min-width:400px\">\n          <div class=\"display:hidden\">Stuff</div>\n              <mat-toolbar class=\"toolbar-white\" style=\"position: absolute; top:0; width: 100%\">\n                <div>Workflow Execution</div>\n                <span class=\"fill-space\"></span>\n              </mat-toolbar>\n              <div style=\"position:absolute; left:0; right:0; top:64px; bottom:0px; overflow-y: auto\">\n                <mat-toolbar style=\"background: none\">\n                  <mat-toolbar-row class=\"b-b\">\n                    <button [hidden]=\"!view.showJSONs\" tabindex=\"-1\" matTooltip=\"Show Workflow Execution JSON\" style=\"display:inline\" class=\"m-r-sm\"\n                      mat-stroked-button (click)=\"showJson(transformQuery(), 'Workflow Execution JSON')\"><i class=\"material-icons\">code</i></button>\n                    <span class=\"fill-space\"></span>\n                    <button tabindex=\"-1\" matTooltip=\"Run Workflows\" style=\"display:inline\" class=\"m-l-sm\" mat-stroked-button matBadge=\"{{selection.selected.length}}\" [disabled]=\"selection.selected.length == 0 || !validateInputMappings()\"\n                      color=\"primary\" (click)=\"doWorkflowExecution()\"><i class=\"material-icons\">play_arrow</i>\n                    </button>\n                  </mat-toolbar-row>\n                </mat-toolbar>\n                <div class=\"clearfix\"></div>\n                <div class=\"m-a\">\n                    <div class=\"font-bold\">Workflow</div>\n                    <mat-form-field class=\"m-b\">\n                      <mat-select [(ngModel)]=\"workflow\">\n                          <mat-option *ngFor=\"let workflow of workflows\" [value]=\"workflow\">\n                            {{workflow.name}}\n                          </mat-option>\n                      </mat-select>\n                    </mat-form-field>\n                    <div class=\"font-bold m-b-sm\">Inputs</div>\n\n                    <div *ngIf=\"!validateInputMappings()\" class=\"m-b-sm\"><div class=\"text-muted\">Assign each input a field from your search</div></div>\n\n                    <div class=\"mat-table mat-elevation-z1 m-b\">\n                    <div class=\"mat-header-row\">\n\n                      <div class=\"mat-header-cell\">\n                          Input\n                      </div>\n                      <div class=\"mat-header-cell\">\n                          Field\n                      </div>\n                      <div class=\"mat-header-cell\" style=\"max-width: 38px\">\n                      </div>\n                    </div>\n                    <div class=\"mat-row\" *ngFor=\"let input of workflow.inputs\">\n\n                        <div class=\"mat-cell\" style=\"display: flex; flex-direction: row\">\n                          {{input.name}}\n                        </div>\n                        <div class=\"mat-cell\">\n                            <mat-form-field>\n                                <mat-select [(ngModel)]=\"input.mappedField\" class=\"mat-input-invalid\">\n                                    <mat-option *ngFor=\"let field of config.fields | keyvalue\" [value]=\"field\">\n                                      {{field.value.name}}\n                                    </mat-option>\n                                </mat-select>\n                            </mat-form-field>\n                        </div>\n                        <div class=\"mat-cell\" style=\"max-width: 38px; padding-left:5px;\">\n                            <i *ngIf=\"!input.mappedField\" class=\"material-icons\" matTooltip=\"Input not mapped\"  style=\"font-size:22px; color:#dc3545;\">error</i>\n                            <i *ngIf=\"input.mappedField && !isFieldSelected(input.mappedField.value)\" matTooltip=\"Field not selected in query\" class=\"material-icons\" style=\"font-size:22px; color:#ffc107;\">error</i>\n                        </div>\n                    </div>\n                  </div>\n                  <div *ngIf=\"runs.length || isSubmittingRuns()\">\n                    <div class=\"font-bold m-b-sm\">Monitor</div>\n                    <div class=\"mat-table mat-elevation-z1\">\n                      <div class=\"mat-header-row\">\n                        <div class=\"mat-header-cell\">\n                          Execution ID\n                        </div>\n                        <div class=\"mat-header-cell\">\n                          Status\n                        </div>\n                      </div>\n                      <mat-progress-bar *ngIf=\"isSubmittingRuns()\" mode=\"indeterminate\"></mat-progress-bar>\n                      <div class=\"mat-row\" *ngFor=\"let run of runs\">\n                        <div class=\"mat-cell\" style=\"display: flex; flex-direction: row\">{{run}}</div>\n                        <div class=\"mat-cell\">\n                          <div *ngIf=\"runStatus[run].state == 'INITIALIZING' || runStatus[run].state == 'RUNNING'\"><mat-spinner [diameter]=\"20\"></mat-spinner></div>\n                          <div *ngIf=\"runStatus[run].state == 'COMPLETE'\"><i class=\"material-icons\" style=\"color:green;\">check</i></div>\n                          <div *ngIf=\"runStatus[run].state == 'CONNECTION ERROR'\">Error</div>\n                        </div>\n                      </div>\n                    </div>\n                  </div>\n                </div>\n              </div>\n      </mat-sidenav>\n      <mat-sidenav-content>\n        <mat-toolbar class=\"toolbar-white\" style=\"height: 64px; position:absolute; top: 0;\">\n          <mat-toolbar-row>\n            <div>Results</div>\n            <span class=\"fill-space\"></span>\n            \n            <button [hidden]=\"!view.showJSONs\" tabindex=\"-1\" matTooltip=\"Show Results JSON\" [hidden]=\"!results\" style=\"display:inline\" class=\"m-r-sm\"\n              mat-stroked-button (click)=\"showJson(results, 'Results JSON')\"><i class=\"material-icons\">code</i></button>\n            <mat-chip-list [hidden]=\"selection.selected.length == 0\" >\n                <mat-chip color=\"primary\">{{selection.selected.length}} selected</mat-chip>\n              </mat-chip-list>\n            <mat-paginator #paginator [length]=\"1000\" [pageSize]=\"query.limit\" [pageSizeOptions]=\"[10, 50, 100]\"\n              (page)=\"pageEvent = paginationChanged($event)\">\n            </mat-paginator>\n            <button tabindex=\"-1\" mat-icon-button matTooltip=\"Wrap cell content\" style=\"display:inline\"\n              (click)=\"view.wrapResultTableCells = !view.wrapResultTableCells\"\n              color=\"{{view.wrapResultTableCells ? 'primary' : '' }}\">\n              <mat-icon>wrap_text</mat-icon>\n            </button>\n          </mat-toolbar-row>\n        </mat-toolbar>\n        <div style=\"position:absolute; width:100%; top:64px; bottom:0; overflow-y: auto\">\n          <mat-progress-bar mode=\"indeterminate\" *ngIf=\"view.isQuerying\"></mat-progress-bar>\n          <div *ngIf=\"!results\"\n            style=\"height: 100%; display: flex; flex-direction:column; justify-content: center; align-items: center; color: #bdbdbd;\">\n            <div>\n              <i class=\"material-icons\" style=\"font-size: 100px !important\">not_interested</i>\n            </div>\n            <div style=\"font-size: 16px !important\">\n              No search results\n            </div>\n          </div>\n\n          <div *ngIf=\"results\" class=\"m-v\">\n            <div id=\"results\" class=\"mat-table m-s mat-elevation-z1\"\n              [ngClass]=\"{ 'no-wrap' : !view.wrapResultTableCells }\">\n              <div class=\"mat-header-row\">\n                <div class=\"mat-header-cell mat-column-select\">\n                    <mat-checkbox (change)=\"$event ? masterToggle() : null\"\n                    [checked]=\"selection.hasValue() && isAllSelected()\"\n                    [indeterminate]=\"selection.hasValue() && !isAllSelected()\">\n                    </mat-checkbox>\n                </div>\n                <div class=\"mat-header-cell\" *ngFor=\"let field of results.fields\">{{field.name}}</div>\n              </div>\n              <div class=\"mat-row\" *ngFor=\"let result of results.results\">\n                <div class=\"mat-cell mat-column-select\">\n                    <mat-checkbox (click)=\"$event.stopPropagation()\"\n                    (change)=\"$event ? selection.toggle(result) : null\"\n                    [checked]=\"selection.isSelected(result)\">\n                    </mat-checkbox>\n                </div>\n                <div class=\"mat-cell\" *ngFor=\"let value of result.values; let i = index\">\n                  <div *ngIf=\"value.field.type == 'json'\">\n                    <button tabindex=\"-1\" mat-stroked-button\n                      (click)=\"showJson(jsonify({'value' : value.value}), value.field.name)\">JSON</button>\n                  </div>\n                  <div *ngIf=\"value.field.type == 'org.ga4gh.drs'\">\n                        {{value.value.name}}\n                        <button mat-stroked-button tabindex=\"-1\" matTooltip=\"Show DRS JSON\"\n                        (click)=\"showJson({'value' : jsonify(value.value)}, getDrsLabel(value.value))\"><i class=\"material-icons\">code</i></button>\n                        <button mat-stroked-button tabindex=\"-1\"  matTooltip=\"Download Object\" class=\"m-l-sm\"\n                        (click)=\"downloadDrs(value.value)\"><i class=\"material-icons\">arrow_downward</i></button>\n                  </div>\n                  <div *ngIf=\"value.field.type != 'json' && value.field.type != 'org.ga4gh.drs'\">\n                    {{value.value}}\n                  </div>\n                </div>\n              </div>\n            </div>\n            <div class=\"m-b\"></div>\n          </div>\n        </div>\n      </mat-sidenav-content>\n    </mat-sidenav-container>\n  </div>\n</div>\n"

/***/ }),

/***/ "./src/app/app.component.scss":
/*!************************************!*\
  !*** ./src/app/app.component.scss ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "/deep/ html {\n  font: 14px sans-serif; }\n\n.font-bold {\n  font-weight: bold; }\n\n.mat-icon-button {\n  outline: none; }\n\n.mat-arrow-icon {\n  outline: none;\n  line-height: 32px; }\n\n.mat-form-field {\n  padding-left: 5px;\n  padding-right: 5px; }\n\n.text-input {\n  padding: 4px 8px;\n  border-radius: 4px;\n  border: 1px solid #ccc; }\n\n.text-area {\n  width: 300px;\n  height: 100px; }\n\n.output {\n  width: 100%;\n  height: 300px; }\n\n.m-l-lg {\n  margin-left: 30px; }\n\n.m-l-sm {\n  margin-left: 10px; }\n\n.m-r-lg {\n  margin-right: 30px; }\n\n.m-r-sm {\n  margin-right: 10px; }\n\n.m-b-lg {\n  margin-bottom: 30px; }\n\n.m-b-sm {\n  margin-bottom: 10px; }\n\n.m-b {\n  margin-bottom: 20px; }\n\n.m-t {\n  margin-top: 20px; }\n\n.m-t-sm {\n  margin-top: 10px; }\n\n.m-a {\n  margin: 20px; }\n\n.m-s {\n  margin: 0 20px 0 20px; }\n\n.m-v {\n  margin: 20px 0 20px 0; }\n\n.h4 {\n  font-size: 16px;\n  margin: 0; }\n\n.h5 {\n  font-size: 14px;\n  margin: 0; }\n\n.b-b {\n  border-bottom: 1px solid #e0e0e0; }\n\n.b-t {\n  border-top: 1px solid #e0e0e0; }\n\n.text-muted {\n  color: #4f4f4f; }\n\n.mat-table {\n  display: block; }\n\n.mat-row,\n.mat-header-row {\n  display: flex;\n  border-bottom-width: 1px;\n  border-bottom-style: solid;\n  border-bottom-color: #CCC;\n  align-items: center;\n  min-height: 48px;\n  padding: 0 24px; }\n\n.mat-cell,\n.mat-header-cell {\n  flex: 1;\n  overflow: hidden;\n  word-wrap: break-word;\n  font-size: 14px; }\n\n.no-wrap .mat-header-cell,\n.no-wrap .mat-header-cell button,\n.no-wrap .mat-cell {\n  display: block;\n  overflow: hidden;\n  text-overflow: ellipsis;\n  white-space: nowrap; }\n\n.mat-row:hover {\n  background-color: #f9f9f9; }\n\n/deep/ .error-snack {\n  color: red; }\n\n/deep/ .success-snack {\n  color: #20d760; }\n\nmat-paginator {\n  background: none !important; }\n\n.fill-space {\n  flex: 1 1 auto; }\n\n.toolbar-white {\n  height: 64px;\n  background-color: white;\n  border-bottom: 1px solid #e0e0e0;\n  z-index: 100; }\n\nhtml, body {\n  width: 100%;\n  height: 100%;\n  margin: 0;\n  padding: 0; }\n\n.full-page {\n  position: absolute;\n  top: 0;\n  left: 0;\n  right: 0;\n  bottom: 0; }\n\n.box-container {\n  top: 0;\n  left: 0;\n  right: 0;\n  bottom: 0;\n  height: 100%;\n  width: 100%;\n  display: flex;\n  flex-direction: column; }\n\n.box-container .box-header {\n    display: flex;\n    flex-direction: row; }\n\n.box-container .box-content {\n    flex: 1; }\n\n.box-container .box-footer {\n    display: flex;\n    flex-direction: row; }\n\na:hover {\n  text-decoration: none; }\n\n.mat-column-select {\n  overflow: initial;\n  max-width: 40px;\n  padding-top: 5px; }\n\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi9Vc2Vycy9qaW0vQ29kZS9OZXRCZWFuc1Byb2plY3RzL2dhNGdoLXNlYXJjaC1hZGFwdGVyLXByZXN0by9mcm9udGVuZC9kZW1vL3NyYy9hcHAvYXBwLmNvbXBvbmVudC5zY3NzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUNBO0VBQ0UscUJBQXFCLEVBQUE7O0FBR3ZCO0VBQ0UsaUJBQWlCLEVBQUE7O0FBR25CO0VBQ0UsYUFBYSxFQUFBOztBQUdmO0VBQ0UsYUFBYTtFQUNiLGlCQUFpQixFQUFBOztBQUduQjtFQUNFLGlCQUFpQjtFQUNqQixrQkFBa0IsRUFBQTs7QUFHcEI7RUFDRSxnQkFBZ0I7RUFDaEIsa0JBQWtCO0VBQ2xCLHNCQUFzQixFQUFBOztBQUd4QjtFQUNFLFlBQVk7RUFDWixhQUFhLEVBQUE7O0FBR2Y7RUFDRSxXQUFXO0VBQ1gsYUFBYSxFQUFBOztBQUdmO0VBQ0UsaUJBQWlCLEVBQUE7O0FBR25CO0VBQ0UsaUJBQWlCLEVBQUE7O0FBR25CO0VBQ0Usa0JBQWtCLEVBQUE7O0FBR3BCO0VBQ0Usa0JBQWtCLEVBQUE7O0FBR3BCO0VBQ0UsbUJBQW1CLEVBQUE7O0FBR3JCO0VBQ0UsbUJBQW1CLEVBQUE7O0FBR3JCO0VBQ0UsbUJBQW1CLEVBQUE7O0FBR3JCO0VBQ0UsZ0JBQWdCLEVBQUE7O0FBR2xCO0VBQ0UsZ0JBQWdCLEVBQUE7O0FBR2xCO0VBQ0UsWUFBWSxFQUFBOztBQUdkO0VBQ0UscUJBQXFCLEVBQUE7O0FBR3ZCO0VBQ0UscUJBQXFCLEVBQUE7O0FBR3ZCO0VBQ0UsZUFBZTtFQUNmLFNBQVMsRUFBQTs7QUFHWDtFQUNFLGVBQWU7RUFDZixTQUFTLEVBQUE7O0FBR1g7RUFDRSxnQ0FDRixFQUFBOztBQUVBO0VBQ0UsNkJBQ0YsRUFBQTs7QUFFQTtFQUNFLGNBQWMsRUFBQTs7QUFHaEI7RUFDRSxjQUFjLEVBQUE7O0FBR2hCOztFQUVFLGFBQWE7RUFDYix3QkFBd0I7RUFDeEIsMEJBQTBCO0VBQzFCLHlCQUF5QjtFQUN6QixtQkFBbUI7RUFDbkIsZ0JBQWdCO0VBQ2hCLGVBQWUsRUFBQTs7QUFHakI7O0VBRUUsT0FBTztFQUNQLGdCQUFnQjtFQUNoQixxQkFBcUI7RUFDckIsZUFBYyxFQUFBOztBQUloQjs7O0VBSU0sY0FBYztFQUNkLGdCQUFnQjtFQUNoQix1QkFBdUI7RUFDdkIsbUJBQW1CLEVBQUE7O0FBSXpCO0VBQ0UseUJBQXlCLEVBQUE7O0FBRzNCO0VBQ0UsVUFBVSxFQUFBOztBQUdaO0VBQ0UsY0FBYyxFQUFBOztBQUdoQjtFQUNFLDJCQUEyQixFQUFBOztBQUc3QjtFQUNFLGNBQWMsRUFBQTs7QUFHaEI7RUFDRSxZQUFZO0VBQ1osdUJBQXVCO0VBQ3ZCLGdDQUFnQztFQUNoQyxZQUFZLEVBQUE7O0FBR2Q7RUFFSSxXQUFXO0VBQ1gsWUFBWTtFQUVaLFNBQVM7RUFDVCxVQUFVLEVBQUE7O0FBR2Q7RUFDRSxrQkFBa0I7RUFDbEIsTUFBTTtFQUNOLE9BQU87RUFDUCxRQUFRO0VBQ1IsU0FBUyxFQUFBOztBQUdYO0VBQ0UsTUFBTTtFQUNOLE9BQU87RUFDUCxRQUFRO0VBQ1IsU0FBUztFQUVULFlBQVk7RUFDWixXQUFXO0VBRVosYUFBYTtFQUNiLHNCQUFzQixFQUFBOztBQVZ2QjtJQWFJLGFBQWE7SUFDYixtQkFBbUIsRUFBQTs7QUFkdkI7SUFrQkUsT0FBTyxFQUFBOztBQWxCVDtJQXNCSSxhQUFhO0lBQ2IsbUJBQW1CLEVBQUE7O0FBSXZCO0VBRUkscUJBQXFCLEVBQUE7O0FBSXpCO0VBQ0UsaUJBQWlCO0VBQ2pCLGVBQWU7RUFDZixnQkFBZ0IsRUFBQSIsImZpbGUiOiJzcmMvYXBwL2FwcC5jb21wb25lbnQuc2NzcyIsInNvdXJjZXNDb250ZW50IjpbIlxuL2RlZXAvIGh0bWwge1xuICBmb250OiAxNHB4IHNhbnMtc2VyaWY7XG59XG5cbi5mb250LWJvbGQge1xuICBmb250LXdlaWdodDogYm9sZDtcbn1cblxuLm1hdC1pY29uLWJ1dHRvbiB7XG4gIG91dGxpbmU6IG5vbmU7XG59XG5cbi5tYXQtYXJyb3ctaWNvbiB7XG4gIG91dGxpbmU6IG5vbmU7XG4gIGxpbmUtaGVpZ2h0OiAzMnB4O1xufVxuXG4ubWF0LWZvcm0tZmllbGQge1xuICBwYWRkaW5nLWxlZnQ6IDVweDtcbiAgcGFkZGluZy1yaWdodDogNXB4O1xufVxuXG4udGV4dC1pbnB1dCB7XG4gIHBhZGRpbmc6IDRweCA4cHg7XG4gIGJvcmRlci1yYWRpdXM6IDRweDtcbiAgYm9yZGVyOiAxcHggc29saWQgI2NjYztcbn1cblxuLnRleHQtYXJlYSB7XG4gIHdpZHRoOiAzMDBweDtcbiAgaGVpZ2h0OiAxMDBweDtcbn1cblxuLm91dHB1dCB7XG4gIHdpZHRoOiAxMDAlO1xuICBoZWlnaHQ6IDMwMHB4O1xufVxuXG4ubS1sLWxnIHtcbiAgbWFyZ2luLWxlZnQ6IDMwcHg7XG59XG5cbi5tLWwtc20ge1xuICBtYXJnaW4tbGVmdDogMTBweDtcbn1cblxuLm0tci1sZyB7XG4gIG1hcmdpbi1yaWdodDogMzBweDtcbn1cblxuLm0tci1zbSB7XG4gIG1hcmdpbi1yaWdodDogMTBweDtcbn1cblxuLm0tYi1sZyB7XG4gIG1hcmdpbi1ib3R0b206IDMwcHg7XG59XG5cbi5tLWItc20ge1xuICBtYXJnaW4tYm90dG9tOiAxMHB4O1xufVxuXG4ubS1iIHtcbiAgbWFyZ2luLWJvdHRvbTogMjBweDtcbn1cblxuLm0tdCB7XG4gIG1hcmdpbi10b3A6IDIwcHg7XG59XG5cbi5tLXQtc20ge1xuICBtYXJnaW4tdG9wOiAxMHB4O1xufVxuXG4ubS1hIHtcbiAgbWFyZ2luOiAyMHB4O1xufVxuXG4ubS1zIHtcbiAgbWFyZ2luOiAwIDIwcHggMCAyMHB4O1xufVxuXG4ubS12IHtcbiAgbWFyZ2luOiAyMHB4IDAgMjBweCAwO1xufVxuXG4uaDQge1xuICBmb250LXNpemU6IDE2cHg7XG4gIG1hcmdpbjogMDtcbn1cblxuLmg1IHtcbiAgZm9udC1zaXplOiAxNHB4O1xuICBtYXJnaW46IDA7XG59XG5cbi5iLWIge1xuICBib3JkZXItYm90dG9tOiAxcHggc29saWQgI2UwZTBlMFxufVxuXG4uYi10IHtcbiAgYm9yZGVyLXRvcDogMXB4IHNvbGlkICNlMGUwZTBcbn1cblxuLnRleHQtbXV0ZWQge1xuICBjb2xvcjogIzRmNGY0Zjtcbn1cblxuLm1hdC10YWJsZSB7XG4gIGRpc3BsYXk6IGJsb2NrO1xufVxuXG4ubWF0LXJvdyxcbi5tYXQtaGVhZGVyLXJvdyB7XG4gIGRpc3BsYXk6IGZsZXg7XG4gIGJvcmRlci1ib3R0b20td2lkdGg6IDFweDtcbiAgYm9yZGVyLWJvdHRvbS1zdHlsZTogc29saWQ7XG4gIGJvcmRlci1ib3R0b20tY29sb3I6ICNDQ0M7XG4gIGFsaWduLWl0ZW1zOiBjZW50ZXI7XG4gIG1pbi1oZWlnaHQ6IDQ4cHg7XG4gIHBhZGRpbmc6IDAgMjRweDtcbn1cblxuLm1hdC1jZWxsLFxuLm1hdC1oZWFkZXItY2VsbCB7XG4gIGZsZXg6IDE7XG4gIG92ZXJmbG93OiBoaWRkZW47XG4gIHdvcmQtd3JhcDogYnJlYWstd29yZDtcbiAgZm9udC1zaXplOjE0cHg7XG59XG5cbi8vIFRydW5jYXRlIHRleHQgaW4gdGFibGVzIHdpdGggZWxsaXBzaXNcbi5uby13cmFwIHtcbiAgICAubWF0LWhlYWRlci1jZWxsLFxuICAgIC5tYXQtaGVhZGVyLWNlbGwgYnV0dG9uLFxuICAgIC5tYXQtY2VsbCB7XG4gICAgICBkaXNwbGF5OiBibG9jaztcbiAgICAgIG92ZXJmbG93OiBoaWRkZW47XG4gICAgICB0ZXh0LW92ZXJmbG93OiBlbGxpcHNpcztcbiAgICAgIHdoaXRlLXNwYWNlOiBub3dyYXA7XG4gICAgfVxuICB9XG5cbi5tYXQtcm93OmhvdmVyIHtcbiAgYmFja2dyb3VuZC1jb2xvcjogI2Y5ZjlmOTtcbn1cblxuL2RlZXAvIC5lcnJvci1zbmFjayB7XG4gIGNvbG9yOiByZWQ7XG59XG5cbi9kZWVwLyAuc3VjY2Vzcy1zbmFjayB7XG4gIGNvbG9yOiAjMjBkNzYwO1xufVxuXG5tYXQtcGFnaW5hdG9yIHtcbiAgYmFja2dyb3VuZDogbm9uZSAhaW1wb3J0YW50O1xufVxuXG4uZmlsbC1zcGFjZSB7XG4gIGZsZXg6IDEgMSBhdXRvO1xufVxuXG4udG9vbGJhci13aGl0ZSB7XG4gIGhlaWdodDogNjRweDtcbiAgYmFja2dyb3VuZC1jb2xvcjogd2hpdGU7XG4gIGJvcmRlci1ib3R0b206IDFweCBzb2xpZCAjZTBlMGUwO1xuICB6LWluZGV4OiAxMDA7XG59XG5cbmh0bWwsIGJvZHlcbntcbiAgICB3aWR0aDogMTAwJTtcbiAgICBoZWlnaHQ6IDEwMCU7XG5cbiAgICBtYXJnaW46IDA7XG4gICAgcGFkZGluZzogMDtcbn1cblxuLmZ1bGwtcGFnZSB7XG4gIHBvc2l0aW9uOiBhYnNvbHV0ZTtcbiAgdG9wOiAwO1xuICBsZWZ0OiAwO1xuICByaWdodDogMDtcbiAgYm90dG9tOiAwO1xufVxuXG4uYm94LWNvbnRhaW5lciB7XG4gIHRvcDogMDtcbiAgbGVmdDogMDtcbiAgcmlnaHQ6IDA7XG4gIGJvdHRvbTogMDtcblxuICBoZWlnaHQ6IDEwMCU7XG4gIHdpZHRoOiAxMDAlO1xuXG5cdGRpc3BsYXk6IGZsZXg7XG5cdGZsZXgtZGlyZWN0aW9uOiBjb2x1bW47XG5cblx0LmJveC1oZWFkZXIge1xuICAgIGRpc3BsYXk6IGZsZXg7XG4gICAgZmxleC1kaXJlY3Rpb246IHJvdztcblx0fVxuXG5cdC5ib3gtY29udGVudCB7XG5cdFx0ZmxleDogMTtcbiAgfVxuXG4gIC5ib3gtZm9vdGVyIHtcbiAgICBkaXNwbGF5OiBmbGV4O1xuICAgIGZsZXgtZGlyZWN0aW9uOiByb3c7XG5cdH1cbn1cblxuYSB7XG4gICY6aG92ZXIge1xuICAgIHRleHQtZGVjb3JhdGlvbjogbm9uZTtcbiAgfVxufVxuXG4ubWF0LWNvbHVtbi1zZWxlY3Qge1xuICBvdmVyZmxvdzogaW5pdGlhbDtcbiAgbWF4LXdpZHRoOiA0MHB4O1xuICBwYWRkaW5nLXRvcDogNXB4O1xufVxuIl19 */"

/***/ }),

/***/ "./src/app/app.component.ts":
/*!**********************************!*\
  !*** ./src/app/app.component.ts ***!
  \**********************************/
/*! exports provided: AppComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppComponent", function() { return AppComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_forms__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/forms */ "./node_modules/@angular/forms/fesm5/forms.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _app_api_service__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./app.api.service */ "./src/app/app.api.service.ts");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _dialog_json_json_dialog__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! ./dialog/json/json-dialog */ "./src/app/dialog/json/json-dialog.ts");
/* harmony import */ var _dialog_fields_fields_dialog_component__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! ./dialog/fields/fields-dialog.component */ "./src/app/dialog/fields/fields-dialog.component.ts");
/* harmony import */ var _app_config_service__WEBPACK_IMPORTED_MODULE_7__ = __webpack_require__(/*! ./app-config.service */ "./src/app/app-config.service.ts");
/* harmony import */ var _angular_cdk_collections__WEBPACK_IMPORTED_MODULE_8__ = __webpack_require__(/*! @angular/cdk/collections */ "./node_modules/@angular/cdk/esm5/collections.es5.js");
/* harmony import */ var rxjs_operators__WEBPACK_IMPORTED_MODULE_9__ = __webpack_require__(/*! rxjs/operators */ "./node_modules/rxjs/_esm5/operators/index.js");










var AppComponent = /** @class */ (function () {
    function AppComponent(app, formBuilder, apiService, dialog, snackBar) {
        this.app = app;
        this.formBuilder = formBuilder;
        this.apiService = apiService;
        this.dialog = dialog;
        this.snackBar = snackBar;
        this.events = [];
        this.runs = [];
        this.runStatus = {};
        this.selection = new _angular_cdk_collections__WEBPACK_IMPORTED_MODULE_8__["SelectionModel"](true, []);
        this.query = {
            select: [
                {
                    table: "pgp_canada",
                    name: "participant_id"
                },
                {
                    table: "pgp_canada",
                    name: "vcf_object"
                }
            ],
            from: 'pgp_canada',
            where: {
                condition: 'and',
                rules: [
                    {
                        "field": "pgp_canada.key",
                        "operator": "=",
                        "value": "Sex"
                    },
                    {
                        "field": "pgp_canada.raw_value",
                        "operator": "=",
                        "value": "F"
                    },
                    {
                        "field": "pgp_canada.chromosome",
                        "operator": "=",
                        "value": "chr1"
                    },
                    {
                        "field": "pgp_canada.start_position",
                        "operator": "=",
                        "value": 5087263
                    },
                    {
                        "field": "pgp_canada.reference_base",
                        "operator": "=",
                        "value": "A"
                    },
                    {
                        "field": "pgp_canada.alternate_base",
                        "operator": "=",
                        "value": "G"
                    }
                ]
            },
            limit: 100,
            offset: 0
        };
        this.config = {
            fields: undefined
        };
        this.view = {
            showJSONs: false,
            leftSidebarOpened: false,
            rightSidebarOpened: false,
            wrapResultTableCells: true,
            isQuerying: false,
            selectedTabIndex: 0,
            queryChanged: false,
            displayQueryComponents: {
                select: true,
                from: true,
                where: true,
                limit: true,
                offset: true
            }
        };
        this.workflows = [
            {
                name: "md5sum",
                inputs: [
                    {
                        id: "input_file",
                        name: "File",
                    }
                ],
                url: "http://localhost:8080/api/workflow/organization/108/project/125/workflowVersion/260"
            },
            {
                name: "DeepVariant",
                inputs: [
                    {
                        id: "input_files",
                        name: "Files",
                    }
                ],
                url: "http://localhost:8080/api/workflow/organization/108/project/125/workflowVersion/260"
            }
        ];
        this.workflow = this.workflows[0];
        this.results = null;
        this.numWorkflowsSubmitted = 0;
        this.sitename = app.config.sitename;
        this.view.showJSONs = app.config.developerMode;
        this.queryCtrl = this.formBuilder.control(this.query.where);
    }
    AppComponent.prototype.transformSelect = function (selectFields) {
        var newSelectFields = [];
        for (var i = 0; i < selectFields.length; i++) {
            newSelectFields.push({ "field": selectFields[i].table + "." + selectFields[i].name });
        }
        return newSelectFields;
    };
    AppComponent.prototype.transformRule = function (rule) {
        if ('condition' in rule) {
            return this.transformCondition(rule);
        }
        else if ('operator' in rule) {
            return this.transformOperator(rule);
        }
        else {
            throw new Error('Unknown rule format');
        }
    };
    AppComponent.prototype.transformCondition = function (rule) {
        var predicates = [];
        for (var _i = 0, _a = rule.rules; _i < _a.length; _i++) {
            var subrule = _a[_i];
            predicates.push(this.transformRule(subrule));
        }
        return {
            'p': rule.condition,
            'predicates': predicates
        };
    };
    AppComponent.prototype.transformOperator = function (rule) {
        var p = {
            'p': rule.operator,
            'rfield': rule.field
        };
        p[this.valueKey(rule.field)] = rule.value;
        return p;
    };
    AppComponent.prototype.valueKey = function (fieldName) {
        var field = this.config.fields[fieldName];
        if (field.type == 'string') {
            return 'lstring';
        }
        else if (field.type == 'number') {
            return 'lnumber';
        }
        else if (field.type == 'boolean') {
            return 'lboolean';
        }
        else {
            return 'lvalue';
        }
    };
    AppComponent.prototype.jsonify = function (str) {
        return JSON.parse(str);
    };
    AppComponent.prototype.paginationChanged = function (event) {
        this.query.limit = event.pageSize;
        this.query.offset = event.pageSize * event.pageIndex;
        this.view.queryChanged = true;
    };
    AppComponent.prototype.transformQuery = function () {
        return {
            'select': this.transformSelect(this.query.select),
            'from': [{
                    'table': this.query.from
                }],
            'where': this.transformRule(this.query.where),
            'limit': this.query.limit,
            'offset': this.query.offset
        };
    };
    AppComponent.prototype.createSearchRequest = function () {
        return { json_query: this.transformQuery() };
    };
    // This is inefficient, being called a lot
    AppComponent.prototype.isFieldSelected = function (field) {
        var tableName = field.table;
        var fieldName = field.name;
        for (var i = 0; i < this.query.select.length; i++) {
            if (this.query.select[i].table == tableName && this.query.select[i].name == fieldName) {
                return true;
            }
        }
        return false;
    };
    AppComponent.prototype.toggleFieldSelection = function (event, field) {
        var fieldName = field.name;
        var checked = event.checked;
        if (checked) {
            this.query.select.push(field);
        }
        else {
            for (var i = 0; i < this.query.select.length; i++) {
                if (this.query.select[i].name == fieldName) {
                    this.query.select.splice(i, 1);
                    return;
                }
            }
        }
        this.view.queryChanged = true;
    };
    AppComponent.prototype.selectAllFields = function (b) {
        if (b) {
            var newSelect = [];
            for (var index in this.config.fields) {
                newSelect.push(this.config.fields[index]);
            }
            this.query.select = newSelect;
        }
        else {
            this.query.select = [];
        }
        this.view.queryChanged = true;
    };
    AppComponent.prototype.showJson = function (jsonObj, title) {
        this.jsonDialogRef = this.dialog.open(_dialog_json_json_dialog__WEBPACK_IMPORTED_MODULE_5__["JsonDialog"], {
            width: '80%',
            height: '80%',
            data: { query: jsonObj, title: title }
        });
    };
    AppComponent.prototype.showFields = function () {
        this.fieldsDialogRef = this.dialog.open(_dialog_fields_fields_dialog_component__WEBPACK_IMPORTED_MODULE_6__["FieldsDialogComponent"], {
            width: '80%',
            height: '80%',
            data: { fields: this.config.fields }
        });
    };
    AppComponent.prototype.getDrsLabel = function (drs) {
        return JSON.parse(drs).name;
    };
    AppComponent.prototype.doWorkflowExecution = function () {
        var _this = this;
        this.numWorkflowsSubmitted += this.selection.selected.length;
        for (var i = 0; i < this.selection.selected.length; i++) {
            var row = this.selection.selected[i].values;
            var params = {};
            for (var j = 0; j < this.workflow.inputs.length; j++) {
                var input = this.workflow.inputs[j];
                var field = input.mappedField;
                var fieldName = field.value.name;
                for (var k = 0; k < row.length; k++) {
                    if (row[k].field.name == fieldName) {
                        params[fieldName] = row[k].value;
                    }
                }
            }
            var wes = {
                workflowUrl: this.workflow.url,
                parameters: params
            };
            this.apiService.getToken(i * 5000).pipe(Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_9__["switchMap"])(function (token) { return _this.apiService.doWorkflowExecution(token, params); })).subscribe(function (_a) {
                var run_id = _a.run_id;
                return _this.startJobMonitor(run_id);
            }, function (_a) {
                var error = _a.error;
                return _this.snackError(error.msg || error.message);
            });
        }
    };
    AppComponent.prototype.isSubmittingRuns = function () {
        return this.numWorkflowsSubmitted > this.runs.length;
    };
    AppComponent.prototype.startJobMonitor = function (runId) {
        var _this = this;
        this.runs.push(runId);
        this.runStatus[runId] = {
            state: 'INITIALIZING'
        };
        return this.apiService.getToken().pipe(Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_9__["switchMap"])(function (token) { return _this.apiService.monitorJob(token, runId); }), Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_9__["delay"])(10000), Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_9__["repeat"])(100), Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_9__["takeWhile"])(function (_a) {
            var state = _a.state;
            return state === 'INITIALIZING' || state === 'RUNNING';
        })).subscribe(function (res) {
            _this.runStatus[runId] = res;
        }, function (_a) {
            var error = _a.error;
            _this.runStatus[runId] = 'CONNECTION ERROR';
            _this.snackError("Error getting run " + runId + " status: " + (error.msg || error.message));
        }, function () {
            _this.runStatus[runId] = { state: 'COMPLETE' };
        });
    };
    AppComponent.prototype.doQuery = function (query) {
        var _this = this;
        this.view.isQuerying = true;
        var searchRequest = this.createSearchRequest();
        this.apiService.doQuery(searchRequest).subscribe(function (dto) {
            _this.view.queryChanged = false;
            _this.results = dto;
            _this.view.isQuerying = false;
            _this.view.selectedTabIndex = 2;
        }, function (err) {
            console.log('Error', err);
            _this.view.isQuerying = false;
        });
    };
    AppComponent.prototype.validateInputMappings = function () {
        for (var j = 0; j < this.workflow.inputs.length; j++) {
            var input = this.workflow.inputs[j];
            if (!input.mappedField) {
                return false;
            }
            else if (!this.isFieldSelected(input.mappedField.value)) {
                return false;
            }
        }
        return true;
    };
    AppComponent.prototype.queryChanged = function ($event) {
        this.view.queryChanged = true;
    };
    AppComponent.prototype.downloadDrs = function (drsStr) {
        var drs = JSON.parse(drsStr);
        var url = drs.urls[0].url;
        window.open(url);
    };
    AppComponent.prototype.normalizeArray = function (array, indexKey) {
        var normalizedObject = {};
        for (var i = 0; i < array.length; i++) {
            var key = array[i][indexKey];
            normalizedObject[key] = array[i];
        }
        return normalizedObject;
    };
    /** Whether the number of selected elements matches the total number of rows. */
    AppComponent.prototype.isAllSelected = function () {
        var numSelected = this.selection.selected.length;
        var numRows = this.results.results.length;
        return numSelected == numRows;
    };
    /** Selects all rows if they are not all selected; otherwise clear selection. */
    AppComponent.prototype.masterToggle = function () {
        var _this = this;
        this.isAllSelected() ?
            this.selection.clear() :
            this.results.results.forEach(function (row) { return _this.selection.select(row); });
    };
    AppComponent.prototype.snack = function (message) {
        this.snackBar.open(message, "Dismiss", {
            panelClass: 'success-snack'
        });
    };
    AppComponent.prototype.snackError = function (message) {
        return this.snackBar.open(message, null, {
            panelClass: 'error-snack'
        });
    };
    AppComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.apiService
            .getFields()
            .subscribe(function (fields) {
            _this.config.fields = _this.normalizeArray(fields, 'id');
            _this.view.selectedTabIndex = 0;
        }, function (err) { return console.log('Error', err); });
    };
    AppComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_2__["Component"])({
            selector: 'app-root',
            template: __webpack_require__(/*! ./app.component.html */ "./src/app/app.component.html"),
            styles: [__webpack_require__(/*! ./app.component.scss */ "./src/app/app.component.scss")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_app_config_service__WEBPACK_IMPORTED_MODULE_7__["AppConfigService"],
            _angular_forms__WEBPACK_IMPORTED_MODULE_1__["FormBuilder"],
            _app_api_service__WEBPACK_IMPORTED_MODULE_3__["ApiService"],
            _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatDialog"],
            _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatSnackBar"]])
    ], AppComponent);
    return AppComponent;
}());



/***/ }),

/***/ "./src/app/app.module.ts":
/*!*******************************!*\
  !*** ./src/app/app.module.ts ***!
  \*******************************/
/*! exports provided: AppModule */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppModule", function() { return AppModule; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_platform_browser__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/platform-browser */ "./node_modules/@angular/platform-browser/fesm5/platform-browser.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _app_routing_module__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./app-routing.module */ "./src/app/app-routing.module.ts");
/* harmony import */ var _app_component__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! ./app.component */ "./src/app/app.component.ts");
/* harmony import */ var ang_jsoneditor__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! ang-jsoneditor */ "./node_modules/ang-jsoneditor/fesm5/ang-jsoneditor.js");
/* harmony import */ var _angular_forms__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! @angular/forms */ "./node_modules/@angular/forms/fesm5/forms.js");
/* harmony import */ var angular2_query_builder__WEBPACK_IMPORTED_MODULE_7__ = __webpack_require__(/*! angular2-query-builder */ "./node_modules/angular2-query-builder/dist/index.js");
/* harmony import */ var _angular_platform_browser_animations__WEBPACK_IMPORTED_MODULE_8__ = __webpack_require__(/*! @angular/platform-browser/animations */ "./node_modules/@angular/platform-browser/fesm5/animations.js");
/* harmony import */ var _angular_common_http__WEBPACK_IMPORTED_MODULE_9__ = __webpack_require__(/*! @angular/common/http */ "./node_modules/@angular/common/fesm5/http.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_10__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _app_config_service__WEBPACK_IMPORTED_MODULE_11__ = __webpack_require__(/*! ./app-config.service */ "./src/app/app-config.service.ts");
/* harmony import */ var _dialog_json_json_dialog__WEBPACK_IMPORTED_MODULE_12__ = __webpack_require__(/*! ./dialog/json/json-dialog */ "./src/app/dialog/json/json-dialog.ts");
/* harmony import */ var _dialog_fields_fields_dialog_component__WEBPACK_IMPORTED_MODULE_13__ = __webpack_require__(/*! ./dialog/fields/fields-dialog.component */ "./src/app/dialog/fields/fields-dialog.component.ts");
/* harmony import */ var ngx_clipboard__WEBPACK_IMPORTED_MODULE_14__ = __webpack_require__(/*! ngx-clipboard */ "./node_modules/ngx-clipboard/fesm5/ngx-clipboard.js");















var AppModule = /** @class */ (function () {
    function AppModule() {
    }
    AppModule = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_2__["NgModule"])({
            declarations: [
                _app_component__WEBPACK_IMPORTED_MODULE_4__["AppComponent"],
                _dialog_json_json_dialog__WEBPACK_IMPORTED_MODULE_12__["JsonDialog"],
                _dialog_fields_fields_dialog_component__WEBPACK_IMPORTED_MODULE_13__["FieldsDialogComponent"]
            ],
            entryComponents: [
                _dialog_json_json_dialog__WEBPACK_IMPORTED_MODULE_12__["JsonDialog"],
                _dialog_fields_fields_dialog_component__WEBPACK_IMPORTED_MODULE_13__["FieldsDialogComponent"]
            ],
            imports: [
                _angular_platform_browser__WEBPACK_IMPORTED_MODULE_1__["BrowserModule"],
                _app_routing_module__WEBPACK_IMPORTED_MODULE_3__["AppRoutingModule"],
                ang_jsoneditor__WEBPACK_IMPORTED_MODULE_5__["NgJsonEditorModule"],
                _angular_platform_browser_animations__WEBPACK_IMPORTED_MODULE_8__["BrowserAnimationsModule"],
                _angular_platform_browser__WEBPACK_IMPORTED_MODULE_1__["BrowserModule"],
                _angular_forms__WEBPACK_IMPORTED_MODULE_6__["FormsModule"],
                _angular_forms__WEBPACK_IMPORTED_MODULE_6__["ReactiveFormsModule"],
                angular2_query_builder__WEBPACK_IMPORTED_MODULE_7__["QueryBuilderModule"],
                _angular_common_http__WEBPACK_IMPORTED_MODULE_9__["HttpClientModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatToolbarModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatTooltipModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatTableModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatTabsModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatButtonToggleModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatSlideToggleModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatButtonModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatCheckboxModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatSelectModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatInputModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatDatepickerModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatDialogModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatNativeDateModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatRadioModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatIconModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatCardModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatSnackBarModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatProgressSpinnerModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatPaginatorModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatSidenavModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatExpansionModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatProgressBarModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatBadgeModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_10__["MatChipsModule"],
                ngx_clipboard__WEBPACK_IMPORTED_MODULE_14__["ClipboardModule"]
            ],
            providers: [
                _app_config_service__WEBPACK_IMPORTED_MODULE_11__["AppConfigService"],
                {
                    provide: _angular_core__WEBPACK_IMPORTED_MODULE_2__["APP_INITIALIZER"],
                    useFactory: function (appConfig) {
                        return function () {
                            return appConfig.loadAppConfig();
                        };
                    },
                    multi: true,
                    deps: [_app_config_service__WEBPACK_IMPORTED_MODULE_11__["AppConfigService"]]
                }
            ],
            bootstrap: [_app_component__WEBPACK_IMPORTED_MODULE_4__["AppComponent"]]
        })
    ], AppModule);
    return AppModule;
}());



/***/ }),

/***/ "./src/app/dialog/fields/fields-dialog.component.html":
/*!************************************************************!*\
  !*** ./src/app/dialog/fields/fields-dialog.component.html ***!
  \************************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<mat-toolbar class=\"toolbar-white\">\n  <mat-toolbar-row>\n    <div>Fields</div>\n    <span class=\"fill-space\"></span>\n    <button [hidden]=\"!view.showJSONs\" matTooltip=\"Show Fields JSON\" tabindex=\"-1\" style=\"display:inline\" mat-stroked-button\n      (click)=\"showJson(fields, 'Fields JSON')\"><i class=\"material-icons\">code</i></button>\n  </mat-toolbar-row>\n</mat-toolbar>\n<table mat-table cdk-focus-start [dataSource]=\"dataTable\" class=\"mat-elevation-z0\">\n  <ng-container matColumnDef=\"id\">\n    <th mat-header-cell *matHeaderCellDef> Id </th>\n    <td mat-cell *matCellDef=\"let element\"> {{element.id}} </td>\n  </ng-container>\n\n  <ng-container matColumnDef=\"name\">\n    <th mat-header-cell *matHeaderCellDef> Name </th>\n    <td mat-cell *matCellDef=\"let element\"> {{element.name}} </td>\n  </ng-container>\n\n  <ng-container matColumnDef=\"type\">\n    <th mat-header-cell *matHeaderCellDef> Type </th>\n    <td mat-cell *matCellDef=\"let element\"> {{element.type}} </td>\n  </ng-container>\n\n  <ng-container matColumnDef=\"value\">\n    <th mat-header-cell *matHeaderCellDef> Value </th>\n    <td mat-cell *matCellDef=\"let element\"> {{element.value}} </td>\n  </ng-container>\n\n  <ng-container matColumnDef=\"specification\">\n    <th mat-header-cell *matHeaderCellDef> Specification </th>\n    <td mat-cell *matCellDef=\"let element\"> <a href=\"{element.specification}\">{{element.specification}}</a> </td>\n  </ng-container>\n\n  <ng-container matColumnDef=\"operators\">\n    <th mat-header-cell *matHeaderCellDef> Operators </th>\n    <td mat-cell *matCellDef=\"let element\"> {{element.operators}} </td>\n  </ng-container>\n\n  <tr mat-header-row *matHeaderRowDef=\"displayedColumns\"></tr>\n  <tr mat-row *matRowDef=\"let row; columns: displayedColumns;\"></tr>\n</table>"

/***/ }),

/***/ "./src/app/dialog/fields/fields-dialog.component.scss":
/*!************************************************************!*\
  !*** ./src/app/dialog/fields/fields-dialog.component.scss ***!
  \************************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "table {\n  width: 100%; }\n\n.toolbar-white {\n  background-color: white;\n  border-bottom: 1px solid #e0e0e0;\n  margin-bottom: 20px; }\n\n.fill-space {\n  flex: 1 1 auto; }\n\nmat-dialog-container {\n  padding: 0px !important; }\n\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi9Vc2Vycy9qaW0vQ29kZS9OZXRCZWFuc1Byb2plY3RzL2dhNGdoLXNlYXJjaC1hZGFwdGVyLXByZXN0by9mcm9udGVuZC9kZW1vL3NyYy9hcHAvZGlhbG9nL2ZpZWxkcy9maWVsZHMtZGlhbG9nLmNvbXBvbmVudC5zY3NzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBO0VBQ0UsV0FBVyxFQUFBOztBQUdiO0VBQ0UsdUJBQXVCO0VBQ3ZCLGdDQUFnQztFQUNoQyxtQkFBbUIsRUFBQTs7QUFHckI7RUFDRSxjQUFjLEVBQUE7O0FBR2hCO0VBQ0UsdUJBQXVCLEVBQUEiLCJmaWxlIjoic3JjL2FwcC9kaWFsb2cvZmllbGRzL2ZpZWxkcy1kaWFsb2cuY29tcG9uZW50LnNjc3MiLCJzb3VyY2VzQ29udGVudCI6WyJ0YWJsZSB7XG4gIHdpZHRoOiAxMDAlO1xufVxuXG4udG9vbGJhci13aGl0ZSB7XG4gIGJhY2tncm91bmQtY29sb3I6IHdoaXRlOyBcbiAgYm9yZGVyLWJvdHRvbTogMXB4IHNvbGlkICNlMGUwZTA7XG4gIG1hcmdpbi1ib3R0b206IDIwcHg7XG59XG5cbi5maWxsLXNwYWNlIHtcbiAgZmxleDogMSAxIGF1dG87XG59XG5cbm1hdC1kaWFsb2ctY29udGFpbmVyIHtcbiAgcGFkZGluZzogMHB4ICFpbXBvcnRhbnQ7XG59Il19 */"

/***/ }),

/***/ "./src/app/dialog/fields/fields-dialog.component.ts":
/*!**********************************************************!*\
  !*** ./src/app/dialog/fields/fields-dialog.component.ts ***!
  \**********************************************************/
/*! exports provided: FieldsDialogComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "FieldsDialogComponent", function() { return FieldsDialogComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _dialog_json_json_dialog__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ../../dialog/json/json-dialog */ "./src/app/dialog/json/json-dialog.ts");
/* harmony import */ var _app_config_service__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! ../../app-config.service */ "./src/app/app-config.service.ts");





var FieldsDialogComponent = /** @class */ (function () {
    function FieldsDialogComponent(app, dialog, data) {
        var _this = this;
        this.app = app;
        this.dialog = dialog;
        this.data = data;
        this.displayedColumns = ['id', 'name', 'type'];
        //this.view.showJSONs = app.config.developerMode;
        console.log(app.config);
        this.view = { showJSONs: app.config.developerMode };
        this.fields = this.data.fields;
        this.dataTable = Object.keys(this.data.fields)
            .map(function (fieldKey) { return _this.data.fields[fieldKey]; });
    }
    FieldsDialogComponent.prototype.showJson = function (jsonObj, title) {
        this.jsonDialogRef = this.dialog.open(_dialog_json_json_dialog__WEBPACK_IMPORTED_MODULE_3__["JsonDialog"], {
            width: '80%',
            height: '80%',
            data: { query: jsonObj, title: title }
        });
    };
    FieldsDialogComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            template: __webpack_require__(/*! ./fields-dialog.component.html */ "./src/app/dialog/fields/fields-dialog.component.html"),
            styles: [__webpack_require__(/*! ./fields-dialog.component.scss */ "./src/app/dialog/fields/fields-dialog.component.scss")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__param"](2, Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Inject"])(_angular_material__WEBPACK_IMPORTED_MODULE_2__["MAT_DIALOG_DATA"])),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_app_config_service__WEBPACK_IMPORTED_MODULE_4__["AppConfigService"],
            _angular_material__WEBPACK_IMPORTED_MODULE_2__["MatDialog"], Object])
    ], FieldsDialogComponent);
    return FieldsDialogComponent;
}());



/***/ }),

/***/ "./src/app/dialog/json/json-dialog.html":
/*!**********************************************!*\
  !*** ./src/app/dialog/json/json-dialog.html ***!
  \**********************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "\n<mat-toolbar class=\"toolbar-white\">\n    <mat-toolbar-row>\n        <div>{{title}}</div>\n      <div class=\"fill-space\"></div>\n      <button mat-icon-button tabindex=\"-1\" matTooltip=\"Expand\" style=\"display:inline\" (click)=\"expand(true)\" class=\"m-r-s\"><mat-icon>unfold_more</mat-icon></button>\n      <button mat-icon-button tabindex=\"-1\" matTooltip=\"Collapse\" style=\"display:inline\" (click)=\"expand(false)\" class=\"m-r-s\"><mat-icon>unfold_less</mat-icon></button>\n      <button mat-icon-button tabindex=\"-1\" matTooltip=\"Copy to clipboard\" style=\"display:inline\" ngxClipboard [cbContent]=\"query | json\"><mat-icon>file_copy</mat-icon></button>\n    </mat-toolbar-row>\n  </mat-toolbar>\n<div cdk-focus-start class=\"m-v\">\n  <json-editor #jsonEditor [data]=\"query\" [options]=\"editorOptions\"></json-editor>\n</div>\n"

/***/ }),

/***/ "./src/app/dialog/json/json-dialog.scss":
/*!**********************************************!*\
  !*** ./src/app/dialog/json/json-dialog.scss ***!
  \**********************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "table {\n  width: 100%; }\n\n.toolbar-white {\n  background-color: white;\n  border-bottom: 1px solid #e0e0e0;\n  margin-bottom: 20px; }\n\n.fill-space {\n  flex: 1 1 auto; }\n\nmat-dialog-container {\n  padding: 0px !important; }\n\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi9Vc2Vycy9qaW0vQ29kZS9OZXRCZWFuc1Byb2plY3RzL2dhNGdoLXNlYXJjaC1hZGFwdGVyLXByZXN0by9mcm9udGVuZC9kZW1vL3NyYy9hcHAvZGlhbG9nL2pzb24vanNvbi1kaWFsb2cuc2NzcyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFBQTtFQUNFLFdBQVcsRUFBQTs7QUFHYjtFQUNFLHVCQUF1QjtFQUN2QixnQ0FBZ0M7RUFDaEMsbUJBQW1CLEVBQUE7O0FBR3JCO0VBQ0UsY0FBYyxFQUFBOztBQUdoQjtFQUNFLHVCQUF1QixFQUFBIiwiZmlsZSI6InNyYy9hcHAvZGlhbG9nL2pzb24vanNvbi1kaWFsb2cuc2NzcyIsInNvdXJjZXNDb250ZW50IjpbInRhYmxlIHtcbiAgd2lkdGg6IDEwMCU7XG59XG5cbi50b29sYmFyLXdoaXRlIHtcbiAgYmFja2dyb3VuZC1jb2xvcjogd2hpdGU7IFxuICBib3JkZXItYm90dG9tOiAxcHggc29saWQgI2UwZTBlMDtcbiAgbWFyZ2luLWJvdHRvbTogMjBweDtcbn1cblxuLmZpbGwtc3BhY2Uge1xuICBmbGV4OiAxIDEgYXV0bztcbn1cblxubWF0LWRpYWxvZy1jb250YWluZXIge1xuICBwYWRkaW5nOiAwcHggIWltcG9ydGFudDtcbn0iXX0= */"

/***/ }),

/***/ "./src/app/dialog/json/json-dialog.ts":
/*!********************************************!*\
  !*** ./src/app/dialog/json/json-dialog.ts ***!
  \********************************************/
/*! exports provided: JsonDialog */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "JsonDialog", function() { return JsonDialog; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var ang_jsoneditor__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ang-jsoneditor */ "./node_modules/ang-jsoneditor/fesm5/ang-jsoneditor.js");




var JsonDialog = /** @class */ (function () {
    function JsonDialog(dialogRef, dialog, data) {
        this.dialogRef = dialogRef;
        this.dialog = dialog;
        this.data = data;
        this.editorOptions = new ang_jsoneditor__WEBPACK_IMPORTED_MODULE_3__["JsonEditorOptions"]();
        this.query = data.query;
        this.title = data.title;
        this.editorOptions.mode = 'view';
        this.editorOptions.mainMenuBar = false;
        this.editorOptions.navigationBar = false;
        this.editorOptions.statusBar = false;
        this.editorOptions.expandAll = false;
    }
    JsonDialog.prototype.expand = function (b) {
        if (b) {
            this.jsonEditor.expandAll();
        }
        else {
            this.jsonEditor.collapseAll();
        }
    };
    JsonDialog.prototype.ngOnInit = function () {
    };
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])('jsonEditor'),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", ang_jsoneditor__WEBPACK_IMPORTED_MODULE_3__["JsonEditorComponent"])
    ], JsonDialog.prototype, "jsonEditor", void 0);
    JsonDialog = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'json-dialog',
            template: __webpack_require__(/*! ./json-dialog.html */ "./src/app/dialog/json/json-dialog.html"),
            styles: [__webpack_require__(/*! ./json-dialog.scss */ "./src/app/dialog/json/json-dialog.scss")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__param"](2, Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Inject"])(_angular_material__WEBPACK_IMPORTED_MODULE_2__["MAT_DIALOG_DATA"])),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_angular_material__WEBPACK_IMPORTED_MODULE_2__["MatDialogRef"],
            _angular_material__WEBPACK_IMPORTED_MODULE_2__["MatDialog"], Object])
    ], JsonDialog);
    return JsonDialog;
}());



/***/ }),

/***/ "./src/environments/environment.ts":
/*!*****************************************!*\
  !*** ./src/environments/environment.ts ***!
  \*****************************************/
/*! exports provided: environment */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "environment", function() { return environment; });
// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.
var environment = {
    production: false,
    apiUrl: 'http://localhost:8080'
};
/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.


/***/ }),

/***/ "./src/main.ts":
/*!*********************!*\
  !*** ./src/main.ts ***!
  \*********************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_platform_browser_dynamic__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/platform-browser-dynamic */ "./node_modules/@angular/platform-browser-dynamic/fesm5/platform-browser-dynamic.js");
/* harmony import */ var _app_app_module__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ./app/app.module */ "./src/app/app.module.ts");
/* harmony import */ var _environments_environment__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./environments/environment */ "./src/environments/environment.ts");




if (_environments_environment__WEBPACK_IMPORTED_MODULE_3__["environment"].production) {
    Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["enableProdMode"])();
}
Object(_angular_platform_browser_dynamic__WEBPACK_IMPORTED_MODULE_1__["platformBrowserDynamic"])().bootstrapModule(_app_app_module__WEBPACK_IMPORTED_MODULE_2__["AppModule"])
    .catch(function (err) { return console.error(err); });


/***/ }),

/***/ 0:
/*!***************************!*\
  !*** multi ./src/main.ts ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(/*! /Users/jim/Code/NetBeansProjects/ga4gh-search-adapter-presto/frontend/demo/src/main.ts */"./src/main.ts");


/***/ })

},[[0,"runtime","vendor"]]]);
//# sourceMappingURL=main.js.map