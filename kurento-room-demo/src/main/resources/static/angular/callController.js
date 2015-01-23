
kurento_room.controller('callController', function ($scope, $window, ServiceParticipant,ServiceRoom, Fullscreen) {
    console.log("callController iniciado");
    $scope.roomName = ServiceRoom.getRoomName();

    $scope.leaveRoom = function () {

        ServiceRoom.getKurento().leaveRoom();

        ServiceParticipant.removeParticipants();

        //redirect to login
        $window.location.href = '#/login';
    };

    window.onbeforeunload = function () {
        ServiceRoom.getKurento().close();
    };

// FullScreen -----------------------------------------------------------------

    $scope.goFullscreen = function () {

        if (Fullscreen.isEnabled())
            Fullscreen.cancel();
        else
            Fullscreen.all();

    };


});

