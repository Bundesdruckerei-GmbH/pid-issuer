/**
 * Revocation service identification
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
/* tslint:disable:no-unused-variable member-ordering */

import { Inject, Injectable, Optional }                      from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams,
         HttpResponse, HttpEvent, HttpParameterCodec, HttpContext 
        }       from '@angular/common/http';
import { CustomHttpParameterCodec }                          from '../encoder';
import { Observable }                                        from 'rxjs';

// @ts-ignore
import { ErrorResponse } from '../models/errorResponse';

// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS }                     from '../variables';
import { Configuration }                                     from '../configuration';



@Injectable({
  providedIn: 'root'
})
export class EidClientIdentificationService {

    protected basePath = 'http://localhost';
    public defaultHeaders = new HttpHeaders();
    public configuration = new Configuration();
    public encoder: HttpParameterCodec;

    constructor(protected httpClient: HttpClient, @Optional()@Inject(BASE_PATH) basePath: string|string[], @Optional() configuration: Configuration) {
        if (configuration) {
            this.configuration = configuration;
        }
        if (typeof this.configuration.basePath !== 'string') {
            const firstBasePath = Array.isArray(basePath) ? basePath[0] : undefined;
            if (firstBasePath != undefined) {
                basePath = firstBasePath;
            }

            if (typeof basePath !== 'string') {
                basePath = this.basePath;
            }
            this.configuration.basePath = basePath;
        }
        this.encoder = this.configuration.encoder || new CustomHttpParameterCodec();
    }


    // @ts-ignore
    private addToHttpParams(httpParams: HttpParams, value: any, key?: string): HttpParams {
        if (typeof value === "object" && value instanceof Date === false) {
            httpParams = this.addToHttpParamsRecursive(httpParams, value);
        } else {
            httpParams = this.addToHttpParamsRecursive(httpParams, value, key);
        }
        return httpParams;
    }

    private addToHttpParamsRecursive(httpParams: HttpParams, value?: any, key?: string): HttpParams {
        if (value == null) {
            return httpParams;
        }

        if (typeof value === "object") {
            if (Array.isArray(value)) {
                (value as any[]).forEach( elem => httpParams = this.addToHttpParamsRecursive(httpParams, elem, key));
            } else if (value instanceof Date) {
                if (key != null) {
                    httpParams = httpParams.append(key, (value as Date).toISOString().substring(0, 10));
                } else {
                   throw Error("key may not be null if value is Date");
                }
            } else {
                Object.keys(value).forEach( k => httpParams = this.addToHttpParamsRecursive(
                    httpParams, value[k], key != null ? `${key}.${k}` : k));
            }
        } else if (key != null) {
            httpParams = httpParams.append(key, value);
        } else {
            throw Error("key may not be null if value is not object or array");
        }
        return httpParams;
    }

    /**
     * consumes the SAML Response from the eID-Server
     * the eID-Server sends the response and we redirect the eID-Client to continue in the authentication flow, TR-03124
     * @param sAMLResponse the SAML Response (long value)
     * @param relayState provides additional information
     * @param sigAlg signature algorithm
     * @param signature signature
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public getSamlConsumer(sAMLResponse: string, relayState: string, sigAlg: string, signature: string, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<any>;
    public getSamlConsumer(sAMLResponse: string, relayState: string, sigAlg: string, signature: string, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<any>>;
    public getSamlConsumer(sAMLResponse: string, relayState: string, sigAlg: string, signature: string, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<any>>;
    public getSamlConsumer(sAMLResponse: string, relayState: string, sigAlg: string, signature: string, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        if (sAMLResponse === null || sAMLResponse === undefined) {
            throw new Error('Required parameter sAMLResponse was null or undefined when calling getSamlConsumer.');
        }
        if (relayState === null || relayState === undefined) {
            throw new Error('Required parameter relayState was null or undefined when calling getSamlConsumer.');
        }
        if (sigAlg === null || sigAlg === undefined) {
            throw new Error('Required parameter sigAlg was null or undefined when calling getSamlConsumer.');
        }
        if (signature === null || signature === undefined) {
            throw new Error('Required parameter signature was null or undefined when calling getSamlConsumer.');
        }

        let localVarQueryParameters = new HttpParams({encoder: this.encoder});
        if (sAMLResponse !== undefined && sAMLResponse !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>sAMLResponse, 'SAMLResponse');
        }
        if (relayState !== undefined && relayState !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>relayState, 'RelayState');
        }
        if (sigAlg !== undefined && sigAlg !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>sigAlg, 'SigAlg');
        }
        if (signature !== undefined && signature !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>signature, 'Signature');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }

        let localVarTransferCache: boolean | undefined = options && options.transferCache;
        if (localVarTransferCache === undefined) {
            localVarTransferCache = true;
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/eid/saml-consumer`;
        return this.httpClient.request<any>('get', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                params: localVarQueryParameters,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                transferCache: localVarTransferCache,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * requests the TC Token for the Trusted Channel
     * we redirect to the eID-Server with a SAML Request as URL parameter
     * @param auth the authentication id as returned from _getAuthenticationUrl_
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public getTcTokenRedirectUrl(auth: string, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'text/xml' | 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<any>;
    public getTcTokenRedirectUrl(auth: string, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'text/xml' | 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<any>>;
    public getTcTokenRedirectUrl(auth: string, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'text/xml' | 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<any>>;
    public getTcTokenRedirectUrl(auth: string, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'text/xml' | 'application/json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        if (auth === null || auth === undefined) {
            throw new Error('Required parameter auth was null or undefined when calling getTcTokenRedirectUrl.');
        }

        let localVarQueryParameters = new HttpParams({encoder: this.encoder});
        if (auth !== undefined && auth !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>auth, 'auth');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'text/xml',
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }

        let localVarTransferCache: boolean | undefined = options && options.transferCache;
        if (localVarTransferCache === undefined) {
            localVarTransferCache = true;
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/eid/tcToken`;
        return this.httpClient.request<any>('get', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                params: localVarQueryParameters,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                transferCache: localVarTransferCache,
                reportProgress: reportProgress
            }
        );
    }

}
