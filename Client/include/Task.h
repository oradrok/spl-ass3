#ifndef TASK_H
#define TASK_H

#include <string>
#include <iostream>
#include <boost/asio.hpp>
#include <boost/thread.hpp>

#include "connectionHandler.h"

using boost::asio::ip::tcp;
using namespace std;


class Task {
private:
    boost::mutex *mutex;
    ConnectionHandler *connectionHandler;
    std::atomic<bool> *isLoggedIn;
public:
    Task(boost::mutex *mutex, ConnectionHandler *connectionHandler, atomic<bool> *pAtomic);

    void run();
};


#endif //TASK_H