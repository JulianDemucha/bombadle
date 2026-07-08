package com.bombadle.service.auth.email;

/**
 * Single reusable HTML shell for OTP emails (logo header, heading, code box, body copy, footer).
 * Inline CSS only + table layout for email-client compatibility. No scripts / no fake copy button.
 */
final class OtpEmailTemplate {

    /** Content id of the inline logo; must match the addInline(...) id in {@link EmailService}. */
    static final String LOGO_CID = "logo";

    private OtpEmailTemplate() {
    }

    static String build(String heading, String intro, String otpCode, long expirationMinutes, String securityNote) {
        return """
                <!DOCTYPE html>
                <html lang="pl">
                <body style="margin:0;padding:0;background-color:#ffffff;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#ffffff;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #3b423d;">
                          <tr>
                            <td align="center" style="background-color:#3b423d;padding:24px;">
                              <img src="cid:%s" alt="Bombadle" width="220" style="display:block;max-width:220px;height:auto;" />
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px 32px 8px 32px;font-family:Arial,Helvetica,sans-serif;color:#11251a;">
                              <h1 style="margin:0 0 16px 0;font-size:22px;color:#11251a;">%s</h1>
                              <p style="margin:0 0 24px 0;font-size:15px;line-height:1.5;color:#333333;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:0 32px;">
                              <div style="display:inline-block;background-color:#eef2ee;border:2px solid #1c3c2a;border-radius:10px;padding:16px 28px;font-family:'Courier New',Courier,monospace;font-size:34px;font-weight:bold;letter-spacing:10px;color:#11251a;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px 4px 32px;font-family:Arial,Helvetica,sans-serif;">
                              <p style="margin:0;font-size:13px;color:#555555;text-align:center;">Kod jest ważny przez %d minut.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px 28px 32px;font-family:Arial,Helvetica,sans-serif;">
                              <p style="margin:0;font-size:12px;line-height:1.5;color:#777777;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="background-color:#3b423d;padding:16px;font-family:Arial,Helvetica,sans-serif;">
                              <p style="margin:0;font-size:12px;color:#ffffff;">Bombadle</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(LOGO_CID, escape(heading), escape(intro), escape(otpCode), expirationMinutes, escape(securityNote));
    }

    /** Minimal HTML escaping for interpolated text (OTP is digits, copy is our own, but be safe). */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
