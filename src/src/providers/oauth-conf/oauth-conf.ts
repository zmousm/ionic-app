import { Injectable } from '@angular/core';
import {AuthenticationMethod} from "../../shared/entities/authenticationMethod";
import {GlobalProvider} from "../global/global";
import {ProviderInfo} from "../../shared/entities/providerInfo";
import {GeteduroamServices} from "../geteduroam-services/geteduroam-services";
import {LoadingProvider} from "../loading/loading";
import {ErrorHandlerProvider} from "../error-handler/error-handler";
import {DictionaryServiceProvider} from "../dictionary-service/dictionary-service-provider.service";
import {NavController} from "ionic-angular";
import {WifiConfirmation} from "../../pages/wifiConfirmation/wifiConfirmation";
import {OauthFlow} from "../../pages/oauthFlow/oauthFlow";
import {ClientCertificatePassphrasePage} from "../../pages/clientCertificatePassphrase/clientCertificatePassphrase";
import {ConfigurationScreen} from "../../pages/configScreen/configScreen";

@Injectable()
export class OauthConfProvider {

  /**
   * Provide info from a certificate
   */
  providerInfo: ProviderInfo;

  /**
   * Authentication method from a certificate
   */
  validMethod: AuthenticationMethod = new AuthenticationMethod();

  constructor(private global: GlobalProvider, private getEduroamServices: GeteduroamServices,
              private loading: LoadingProvider, private errorHandler: ErrorHandlerProvider,
              private dictionary: DictionaryServiceProvider, public navCtrl: NavController) {
  }

  /**
   * Method to check form, create connection with plugin WifiEapConfigurator and navigate.
   */
  async checkForm(passphrase? : string) {
    this.loading.createAndPresent();
    this.validMethod = this.global.getAuthenticationMethod();
    this.providerInfo = this.global.getProviderInfo();
    if (typeof passphrase !== 'undefined') {
      this.validMethod.clientSideCredential.passphrase = passphrase;
    }
    let config = this.configConnection();
    const checkRequest = await this.getEduroamServices.connectProfile(config);
    this.loading.dismiss();

    if (checkRequest.message.includes('success') || checkRequest.message.includes('error.network.linked')) {
      await this.navigateTo();
    }else if (checkRequest.message.includes('error.network.alreadyAssociated')) {
      await this.errorHandler.handleError(
          this.dictionary.getTranslation('error', 'duplicate'), false, '', 'retryConfiguration', true);
      await this.navCtrl.setRoot(ConfigurationScreen);
    }else if (checkRequest.message.includes('error.network.mobileconfig')) {
      await this.errorHandler.handleError(
          this.dictionary.getTranslation('error', 'mobileconfig'), false, '', '', true);
    } else if (checkRequest.message.includes('error.network.userCancelled')) {
      await this.navCtrl.pop();
    } else {
      await this.errorHandler.handleError(this.dictionary.getTranslation('error', 'invalid-eap'), false, '', 'retryConfiguration', true);
      await this.navCtrl.setRoot(ConfigurationScreen);
    }
  }

  /**
   * Method to create configuration to plugin WifiEapConfigurator
   */
  configConnection() {
    return {
      ssid: [],
      username: '',
      password: '',
      eap: parseInt(this.validMethod.eapMethod.type.toString()),
      servername: this.validMethod.serverSideCredential.serverID,
      auth: null,
      anonymous: this.validMethod.clientSideCredential.anonymousIdentity,
      caCertificate: this.validMethod.serverSideCredential.ca,
      clientCertificate: this.validMethod.clientSideCredential.clientCertificate,
      passPhrase: this.validMethod.clientSideCredential.passphrase
    };
  }

  /**
   * Navigation to the next view
   */
  async navigateTo() {

    !!this.providerInfo.providerLogo ? await this.navCtrl.push(WifiConfirmation, {
          logo: this.providerInfo.providerLogo}, {  animation: 'transition'  }) :
        await this.navCtrl.push(WifiConfirmation, {}, {animation: 'transition'});
  }

}
