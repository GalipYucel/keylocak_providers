<#import "/assets/icons/eye.ftl" as iconEye>
<#import "/assets/icons/eye-slash.ftl" as iconEyeSlash>

<#macro
  kw
  autofocus=false
  class="block border-secondary-200 mt-1 rounded-md w-full focus:border-primary-300 focus:ring focus:ring-primary-200 focus:ring-opacity-50 sm:text-sm"
  disabled=false
  invalid=false
  label=""
  message=""
  name=""
  required=true
  type="text"
  rest...
>
  <div>
    <label class="sr-only" for="${name}">
      ${label}
    </label>
    <#if type == "password">
		<div class="relative">
		  <input
			<#if autofocus>autofocus</#if>
			<#if disabled>disabled</#if>
			<#if required>required</#if>
			
			aria-invalid="${invalid?c}"
			class="${class}"
			id="${name}"
			name="${name}"
			placeholder="${label}"
			type="password"
			onkeyup="toggleInputType(this, '${name}', event)"
			onblur="restoreInputType(this, '${name}')"
			<#list rest as attrName, attrValue>
			  ${attrName}="${attrValue}"
			</#list>
		  >
		  <button
			onclick="toggleVisibility('${name}')"
			aria-controls="${name}"
			aria-expanded="false"
			class="absolute text-secondary-400 right-3 top-3 sm:top-2"
			type="button"
		  >
			<span id="${name}-icon-eye" style="display:block;">
			  <@iconEye.kw />
			</span>
			<span id="${name}-icon-eye-slash" style="display:none;">
			  <@iconEyeSlash.kw />
			</span>
		  </button>
		</div>

		<script>
		  function toggleVisibility(inputId) {
			var input = document.getElementById(inputId);
			var isPassword = input.type === 'password';
			input.type = isPassword ? 'text' : 'password';

			document.getElementById(inputId + '-icon-eye').style.display = isPassword ? 'none' : 'block';
			document.getElementById(inputId + '-icon-eye-slash').style.display = isPassword ? 'block' : 'none';
		  }

		  function toggleInputType(input, inputId, event) {
			var isShowingText = input.type === 'text';
			var isTabKey = event.keyCode === 9;
			if (isShowingText && isTabKey) {
			  document.getElementById(inputId + '-icon-eye-slash').style.display = 'block';
			}
		  }

		  function restoreInputType(input, inputId) {
			if (input.type === 'text') {
			  input.type = 'password';
			  document.getElementById(inputId + '-icon-eye').style.display = 'block';
			  document.getElementById(inputId + '-icon-eye-slash').style.display = 'none';
			}
		  }
		</script>

    <#else>
      <input
        <#if autofocus>autofocus</#if>
        <#if disabled>disabled</#if>
        <#if required>required</#if>

        aria-invalid="${invalid?c}"
        class="${class}"
        id="${name}"
        name="${name}"
        placeholder="${label}"
        type="${type}"

        <#list rest as attrName, attrValue>
          ${attrName}="${attrValue}"
        </#list>
      >
    </#if>
    <#if invalid?? && message??>
      <div class="mt-2 text-red-600 text-sm">
        ${message?no_esc}
      </div>
    </#if>
  </div>
</#macro>
