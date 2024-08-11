<#import "template.ftl" as layout>
<#import "components/atoms/button.ftl" as button>
<#import "components/atoms/button-group.ftl" as buttonGroup>
<#import "components/atoms/checkbox.ftl" as checkbox>
<#import "components/atoms/form.ftl" as form>
<#import "components/atoms/input.ftl" as input>
<#import "components/atoms/link.ftl" as link>

<@layout.registrationLayout displayInfo=true; section>
    <#if section = "show-username">
            <div class="flex items-center justify-center mb-4 space-x-2 font-medium">
			      ${msg(headerText)}
              
            </div>
    <#elseif section = "header">
            <div class="flex items-center justify-center mb-4 space-x-2 font-medium">
			      ${msg(headerText)}
              
            </div>
    <#elseif section == "form">
	 
        <@form.kw
            id="kc-email-totp-code-login-form"
            action="${url.loginAction}"
            method="post"
        >
            <@input.kw
                autofocus=true
                label=msg("emailTOTPFormLabel")
                name="code"
                type="number"
            />

            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span>
                            <@link.kw href="${url.loginUrl}">
                                ${kcSanitize(msg("backToLogin"))?no_esc}
                            </@link.kw>
                        </span>
                    </div>
                </div>

         
                  <@buttonGroup.kw>
          <@button.kw color="primary" name="login" type="submit">
            ${msg("doSubmit")}
          </@button.kw>
        </@buttonGroup.kw>
              
            </div>
        </@form.kw>
    <#elseif section == "info">
        ${msg("emailTOTPFormInstruction")}
    </#if>
</@layout.registrationLayout>
