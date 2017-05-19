$(document).ready($(function () {

    var $form = $("form");
    var $submissionButton = $("form button[type=submit]");

    $submissionButton.on('click', function (e) {
        if (typeof ga === "function" && satisfactionSelection != undefined) {
            e.preventDefault();
            var satisfactionSelection = $('input[name=satisfaction]:checked').val();
            ga('send', 'event', 'itsa-exit-survey', 'satisfaction', satisfactionSelection, {
                hitCallback: function () {
                    $form.submit();
                }
            });
        }
    });

}));