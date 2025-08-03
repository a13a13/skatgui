#ifndef PLAYERDISPATCHER_H
#define PLAYERDISPATCHER_H

#include <map>
#include <string>

#include "Wrapper.h"
#include "Player.h"

// access to singleton

class PlayerDispatcher;

extern "C" {
  extern PlayerDispatcher* getDispatcher();
}

class PlayerDispatcher
{
public:

  PlayerDispatcher()
  {
    std::cout << "dispatcher created @ " << this << std::endl;
  }
  
  virtual ~PlayerDispatcher()
  {
    PlayerMap::iterator it = players.begin(), end = players.end();

    for (; it != end; it++) {
      delete it->second;
      it->second = 0;
    }

    std::cout << "dispatcher destroyed @ " << this << std::endl;
  }

  // store player prototype that is used to create new player instances
  // make sure player is not deleted!
  void addPrototype(Player *player)
  {
    std::cout << "add prototype: " << player->getLibrary() << std::endl;
    
    std::string key =  playerKey(player->getLibrary(), "", "", ""); // not created in any actual game
    PlayerMap::iterator it = players.find(key), end = players.end();

    if (it != end) {
      // already added?
      std::cout << "lib " + player->getLibrary() + " already added to dispatcher" << std::endl;
    }

    players[key] = player;

    std::cout << "map now:" << std::endl;
    
    it = players.begin(), end = players.end();

    for (; it != end; it++) {
      std::cout << it->second->getLibrary() << std::endl;
    }

    std::cout << "done." << std::endl;
  }

  
  std::string computeMove(const std::string &lib,
                          const std::string &type,
                          const std::string &params,
                          const std::string &table,
                          const Wrapper::SimpleGame &game,
                          const double remainingTime)
  {
    // std::cout << "COMPUTE MOVE " << table << std::endl;    
    return findPlayer(lib, type, params, table)->computeMove(game, remainingTime);
  }
  
  void interruptMoveComputation(const std::string & /*lib*/,
                                const std::string & /*type*/,
                                const std::string & /*params*/,
                                const std::string & /*table*/)
  {
    std::cout << "implement interrupt" << std::endl;
  }

  void reset(const std::string &lib,
             const std::string &type,
             const std::string &params,
             const std::string &table)
  {
    //std::cout << "RESET " << lib_type_params << " " << table << std::endl;
    findPlayer(lib, type, params, table)->reset();
  }

  void gameChange(const std::string &lib,
                  const std::string &type,
                  const std::string &params,
                  const std::string &table,
                  const Wrapper::SimpleGame &game)
  {
    // search the player on a table
    Player* player = findPlayer(lib, type, params, table);

    // std::cout << "GAMECHANGE <" << table << "> " << this << " " << player << std::endl;
    player->gameChange(game);
  }
  
  void dispose(const std::string &lib,
               const std::string &type,
               const std::string &params,
               const std::string &table)
  {
    std::cout << "DISPOSE " << table << std::endl;
    
    PlayerMap::iterator it = players.find(playerKey(lib, type, params, table));
    if (it == players.end()) return; // not there

    delete it->second;
    it->second = 0;

    players.erase(it);
  }

  int DDS(const std::string &lib,
          const std::string &type,
          const std::string &params,
          const std::string &table,
          int fh, int mh, int rh,
          int toMove,
          int declarer,
          int gameType,
          int trickCardNum,
          int trickCard1, int trickCard2,
          int ptsDeclarer, int ptsDefenders,
          int alpha, int beta,
          int clearHash)

  {
    // std::cout << "COMPUTE MOVE " << table << std::endl;    
    return findPlayer(lib, type, params, table)->DDS(fh, mh, rh,
                                                     toMove,
                                                     declarer,
                                                     gameType,
                                                     trickCardNum,
                                                     trickCard1, trickCard2,
                                                     ptsDeclarer, ptsDefenders,
                                                     alpha, beta,
                                                     clearHash);
  }
  
private:

  // finds player with key  lib:type:params:table
  // if it doesn't exist create new lib player
  // asserts that a lib player already exists (is added when loading library)
  Player* findPlayer(const std::string &lib,
                     const std::string &type,
                     const std::string &params,
                     const std::string &table)
  {
    std::string key = playerKey(lib, type, params, table);
    PlayerMap::iterator it = players.find(key), end = players.end();

    if (it != end) {
      return it->second; // found
    }
      
    // player not found: find existing player with the same library
    // and create a new player from this library with type,params

    it = players.begin();

    for (; it != end; it++) {
      std::cout << "find " << lib << " " << it->second->getLibrary() << std::endl;
      if (it->second->getLibrary() == lib) 
        break;
    }

    if (it == end) { std::cerr << "can't find player with library " << lib << std::endl; exit(20); }

    // create player object
    Player* player = it->second->newPlayer(type, params);
    
    // and store it under key
    players[key] = player;
    return player;
  }

  std::string playerKey(const std::string &lib,
                        const std::string &type,
                        const std::string &params,
                        const std::string &table)
  {
    return lib + ":" + type + ":" + params + ":" + table;
  }
  
  // data

  // lib,type,params,context -> player
  typedef std::map<std::string, Player*> PlayerMap;
  PlayerMap players;
};


#endif
