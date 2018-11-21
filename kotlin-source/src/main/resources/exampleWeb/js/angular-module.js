"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('auctionAppModule', ['ui.bootstrap','ngLoadingOverlay']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('AuctionAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/auction/";
    let peers = [];
    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'createAuctionModal.html',
            controller: 'CreateAuctionCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });
        modalInstance.result.then(() => {}, () => {});
    };


    demoApp.openBidModal = (auctionId) => {
       demoApp.currentAuctionId=auctionId;
       const modalInstance1 = $uibModal.open({
            templateUrl: 'BidModal.html',
            controller: 'BidModalCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });
        modalInstance1.result.then(() => {}, () => {});
    };

    demoApp.openTransactionDetailsModal = (auctionId) => {
       demoApp.currentAuctionId=auctionId;
       const modalInstance1 = $uibModal.open({
            templateUrl: 'TransactionDetailsModal.html',
            controller: 'TransactionDetailsAuctionCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });
        modalInstance1.result.then(() => {}, () => {});
    };

    demoApp.closeAuction = (auctionId) => {
        demoApp.auctionId=auctionId;
        const modalInstance1 = $uibModal.open({
            templateUrl: 'closeAuctionModal.html',
            controller: 'CloseAuctionCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });
        modalInstance1.result.then(() => {}, () => {});

    };

    demoApp.getAuctions = () => $http.get(apiBaseURL + "auctions")
        .then((response) => demoApp.auctions = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getAuctions();

});


app.controller('CreateAuctionCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers, $loadingOverlay) {
    const modalInstance = this;
    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;
    // Validate and create Auction.
    modalInstance.create = () => {
        $loadingOverlay.show('Transaction Processing ...', 'rgba(0, 0, 0, 0.3)', '#fff');
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;
            $uibModalInstance.close();

            var AuctionParticipants = 'None';
            if (modalInstance.form.auctionType === 'Private'){
                AuctionParticipants = modalInstance.form.auctionParticipants.join('$');
            }

            const createAuctionEndpoint = `${apiBaseURL}create-auction?itemName=${modalInstance.form.itemName}&ItemDescription=${modalInstance.form.itemDescription}&startPrice=${modalInstance.form.startPrice}&ExpiryDate=${modalInstance.form.expiryDate}&AuctionParticipants=${AuctionParticipants}`;
            // Create Auction and handle success / fail responses.
            $http.put(createAuctionEndpoint).then(
                (result) => {
                    $loadingOverlay.hide();
                    modalInstance.displayMessage(result);
                    demoApp.getAuctions();
                },
                (result) => {
                    $loadingOverlay.hide();
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create Auction modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Auction.
    function invalidFormInput() {
        return false;
    }
});


app.controller('BidModalCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers, $loadingOverlay) {
    const modalInstance = this;
    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;
    
    // Validate and create Bid.
    modalInstance.create = () => {
        $loadingOverlay.show('Transaction Processing ...', 'rgba(0, 0, 0, 0.3)', '#fff');
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;
            $uibModalInstance.close();

            const createAuctionEndpoint = `${apiBaseURL}make-bid?amount=${modalInstance.form.amount}&AuctionReference=${demoApp.currentAuctionId}`;
            
            // Create Bid and handle success / fail responses.
            $http.put(createAuctionEndpoint).then(
                (result) => {
                    $loadingOverlay.hide();
                    modalInstance.displayMessage(result);
                    demoApp.getAuctions();
                },
                (result) => {
                    $loadingOverlay.hide();
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Validate the Bid.
    function invalidFormInput() {
        return false;
    }

    // Close create Trade modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

});


app.controller('CloseAuctionCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers, $loadingOverlay) {
    const modalInstance = this;
    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    modalInstance.create = () => {
        $loadingOverlay.show('Transaction Processing ...', 'rgba(0, 0, 0, 0.3)', '#fff');
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;
            $uibModalInstance.close();

            const endAuctionEndpoint = `${apiBaseURL}end-auction?AuctionReference=${demoApp.auctionId}`;

            // End Auction and handle success / fail responses.
            $http.put(endAuctionEndpoint).then(
                (result) => {
                    $loadingOverlay.hide();
                    modalInstance.displayMessage(result);
                    demoApp.getAuctions();
                },
                (result) => {
                    $loadingOverlay.hide();
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Validate the Bid.
    function invalidFormInput() {
        return false;
    }

    // Close create Trade modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

});


app.controller('TransactionDetailsAuctionCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;
    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;
    $http.get(apiBaseURL + "get-auction?linearID="+demoApp.currentAuctionId).then(
        (response) => modalInstance.transactionDetails = response.data
        );
    // Close create Auction modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});