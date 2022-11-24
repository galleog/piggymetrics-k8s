let keycloak = Keycloak('keycloak.json');

function onAuthSuccess() {
    $.ajax({
        url: 'accounts/current',
        datatype: 'json',
        type: 'get',
        headers: {'Authorization': 'Bearer ' + keycloak.token},
        success: function (data) {
            showGreetingPage(data);
        },
        error: function () {
            alert("Failed to get the account for user '" + keycloak.tokenParsed.preferred_username +
                "'. Please, make sure the account exists");
            logout();
        }
    });
}

keycloak.onAuthSuccess = onAuthSuccess;

keycloak.onAuthError = function () {
    $("#preloader").hide();
    flipForm();
    $('.frontforms').val('');
    $("#frontloginform").focus();
    alert("Something went wrong. Please, log in again");
};

keycloak.init({onLoad: 'check-sso'}).success(function (authenticated) {
    if (authenticated) {
        onAuthSuccess();
    } else {
        showLoginForm();
    }
}).error(function () {
    window.location.reload();
});

/**
 * Login
 */
$(".loginbutton").bind("click", function () {
    $("#piggy").toggleClass("loadingspin");
    $("#preloader, #lastlogo").show();

    keycloak.login();
});

/**
 * Logout
 */
function logout() {
    if (keycloak.authenticated) {
      keycloak.logout();
    } else {
      window.location.reload();
    }
}

/**
 * Demo
 */
$(".demobutton").bind("click", function(){
    $.ajax({
        url: 'accounts/demo',
        datatype: 'json',
        type: 'get',
        success: function (data) {
            global.savePermit = false;
            initAccount(data);

            var userAvatar = $("<img />").attr("src","images/userpic.jpg");
            $(userAvatar).load(function() {
                setTimeout(initGreetingPage, 500);
            });
        },
        error: function () {
            alert("Something went wrong. Please, try again");
        }
    });
});

$("#skipmail").bind("click", function(){
    $("#lastlogo").show();
    setTimeout(initGreetingPage, 300);
});

/**
 * Login form effects
 */

function initialShaking(){
	autoShake();
	setTimeout(autoShake, 1900);
}

function autoShake() {
	$("#piggy").toggleClass("auto-shake");
}

function OnHoverShaking() {
	hoverShake();
	setTimeout(hoverShake, 1700);
}

function hoverShake() {
	$("#piggy").toggleClass("hover-shake");
}

function toggleInfo() {
	$("#infopage").toggle();
}

function flipForm() {
	$("#cube").toggleClass("flippedform");
	$("#frontpasswordform").focus();
}

$("#piggy").on("click mouseover", function(){
	if ($(this).hasClass("skakelogo") === false && $(this).hasClass("hover-shake") === false) {
		OnHoverShaking();
	}
});

$(".fliptext").bind("click", function(){
	setTimeout( function() { $("#plusavatar").addClass("avataranimation"); } , 1000);
	$("#flipper").toggleClass("flippedcard");
});

$(".flipinfo").on("click", function() {
	$("#flipper").toggleClass("flippedcardinfo");
	toggleInfo();
});

$(".frominfo, #infotitle, #infosubtitle").on("click", function() {
	$("#flipper").toggleClass("flippedcardinfo");
	setTimeout(toggleInfo, 400);
});
