package helpers.servicemocks

import com.github.tomakehurst.wiremock.client.WireMock._

object AuditStub {
  def stubAuditing(): Unit = {
    stubFor(post(urlMatching("/write/audit"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"x":2}""")
      )
    )
  }
}
