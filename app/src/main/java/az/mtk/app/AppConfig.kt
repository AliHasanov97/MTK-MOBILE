package az.mtk.app

/**
 * Backend + OpenID Provider configuration.
 *
 * Deployed stack (see MTK-UI README):
 *  - MTK API      : .NET modular monolith
 *  - Keycloak     : realm "mtk", public client "mtk-web" (Authorization Code + PKCE)
 *
 * If the API or Keycloak moves, this is the only file that needs to change.
 */
object AppConfig {
    const val ApiBaseUrl = "https://mtk-api.vahid.az/"
    const val OidcIssuer = "https://auth.vahid.az/realms/mtk"
    const val ClientId = "mtk-web"

    /** Must match "Valid redirect URIs" on the Keycloak client and the manifest intent filter. */
    const val RedirectScheme = "az.mtk.app"
    const val RedirectUri = "$RedirectScheme:/oauth2redirect"

    const val Scope = "openid profile email"

    // Keycloak exposes these at fixed paths under the realm issuer.
    const val AuthorizationEndpoint = "$OidcIssuer/protocol/openid-connect/auth"
    const val TokenEndpoint = "$OidcIssuer/protocol/openid-connect/token"
    const val EndSessionEndpoint = "$OidcIssuer/protocol/openid-connect/logout"

    /** Keycloak account console (MTK-UI opens the same URL from its user menu). */
    const val AccountEndpoint = "$OidcIssuer/account"
}
