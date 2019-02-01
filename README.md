# Auction CorDapp
This CorDapp is an example of how Auctions can be performed in a decentralized manner. Auction can be Open to all or Private (accessible only to selected few parties).

More about this usecase [here](https://medium.com/auctionity/making-auctions-safer-faster-and-more-reliable-through-blockchain-technology-part-1-2-5b01bf641b48)


## Instructions for setting up

1. `git clone https://github.com/vardan10/cordapp-auction.git`
2. `cd cordapp-auction`
3. `./gradlew deployNodes` - building may take upto a minute (it's much quicker if you already have the Corda binaries).  
4. `./kotlin-source/build/nodes/runnodes`

At this point you will have a notary node running as well as three other nodes and their corresponding webservers. There should be 7 console windows in total. One for the notary and two for each of the three nodes. The nodes take about 20-30 seconds to finish booting up.


## Using the CorDapp via the UI:
1. PartyA: `localhost:10009/web/example/`
2. PartyB: `localhost:10012/web/example/`
3. PartyC: `localhost:10015/web/example/`


## Using the CorDapp via the console:

1. Start the Auction
In PartyA Console type:
```
start StartAuction itemName: "test", ItemDescription: "test", startPrice: 99, ExpiryDate: "2018-11-16T08:25:25.510045Z", AuctionParticipants: "O=PartyB,L=New York,C=US"
```

2. Get Linear Id of Auction
In any Console (Party A,B or C) type:
```
run vaultQuery contractStateType: com.example.state.Auction
```
Above will output a auction LinearId

3. Make A Bid
In PartyB or PartyC Console type:
```
start MakeBid amount: 120, AuctionReference: "<LinearId>"
```

4. End A Auction
In PartyAConsole type:
```
start EndAuction AuctionReference: "<LinearId>"
```

## Using the CorDapp via the HTTP API's:
1. Create Auction:
```
curl -X PUT 'http://localhost:10009/api/auction/create-auction?itemName=test&ItemDescription=test&startPrice=99&ExpiryDate=2018-11-16T08:25:25.510045Z&AuctionParticipants=O=PartyB,L=New%20York,C=US'
```

2. List Auctions:
```
curl -X GET 'http://localhost:10009/api/auction/auctions'
```

3. Make Bid:
```
curl -X PUT 'http://localhost:10012/api/auction/make-bid?amount=120&AuctionReference=<LINEAR_ID>'
```

4. End Auction:
```
curl -X PUT 'http://localhost:10009/api/auction/end-auction?AuctionReference=<LINEAR_ID>'
```

# ToDo's
Unit Tests


Please Feel free to submit PR's with any Upgrade/Modification.
