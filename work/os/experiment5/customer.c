#ifndef _CUS_CPP
#define _CUS_CPP

#include <stdio.h>
#include <string.h>
#include <sys/ipc.h>
#include <sys/types.h>
#include <sys/shm.h>
#include <sys/sem.h>
#include <sys/msg.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/msg.h>
#include <error.h>
#include <sys/wait.h>
#include <math.h>
#include <time.h>

#define SIZE 1024
#define NUM 4

enum type{
    SOFA,
    WAIT,
    PEOPLE,
    S_W_LOCK,
    BARBE,
    CHARGE,
    IN_SOFA,
    LEAVE,
};

union semun{
    int val;
    struct semid_ds * buf;
    unsigned short * array;
};

struct msgbuf{
    long type;
    char mtext[1];
    int id;
};

key_t get_key( char const  * const address , int value){
    return ftok(address , value);
}

int P(int semid , int index);

int V(int semid , int index);

void show(int * address , int limit){
    for(int i = 0 ; i < limit ; i++){
        printf("Get the value %d for %d .\n" , i , address[i]);
    }
}

int msg_length(key_t msgid);

int const size = sizeof(char) + sizeof(int);

int main(int argc , char ** argv){

    srand((int)(time(0)));
    int const num = 8;

    key_t key_shm = get_key("./" , 0x12);
    key_t key_sem = get_key("./" , 0x13);
    key_t key_sofa = get_key("./" , 0x14);
    key_t key_wait = get_key("./" , 0x15);
    key_t key_charge = get_key("./" , 0x16);
    key_t key_barber = get_key("./" , 0x17);

    int shmid , semid , msg_sofa , msg_wait , msg_charge , msg_barber;

    shmid = shmget(key_shm , 4 , IPC_CREAT | IPC_EXCL);
    if(shmid < 0){
        perror("The shm not found.");
    }
    semid = semget(key_sem , num , IPC_CREAT | IPC_EXCL);
    if(semid < 0){
        perror("The sem not found.");
    }
    msg_sofa = msgget(key_sofa , IPC_CREAT | IPC_EXCL);
    if(msg_sofa < 0){
        perror("Error for msgget.");
    }
    msg_wait = msgget(key_wait , IPC_CREAT | IPC_EXCL);
    if(msg_wait < 0){
        perror("Error for msgget.");
    }
    msg_charge = msgget(key_charge , IPC_CREAT | IPC_EXCL);
    if(msg_charge < 0){
        perror("Error for msgget.");
    }
    msg_barber = msgget(key_barber, IPC_CREAT | IPC_EXCL);

    union semun sender;
    sender.val = 0;
    /*
      SOFA,
      WAIT,
      PEOPLE,
      S_W_LOCK,
      BARBE,
      CHARGE,
      IN_SOFA,
      LEAVE,
    */
    int array[num] = {4 ,13 ,20 ,1 ,0 ,1 ,0};
    for(int i = 0 ; i < num; i++){
        sender.val = array[i];
        if(semctl(semid , i , SETVAL , sender)){
            printf("catch the info for %d.\n" , i);
            perror("Error for set the value .");
            return EXIT_FAILURE;
        }
    }

    pid_t son = fork();
    if(son < 0){
        perror("Error for fork.");
        return EXIT_FAILURE;
    }else if(son == 0){
        //code for son.
        while(1){
            P(semid , PEOPLE); // waiting the people which < 20.
            P(semid , S_W_LOCK); // lock the sofa_wait lock.
            if(msg_length(msg_sofa) < 4){
                struct msgbuf buf;
                buf.id = getpid();
                if(msgsnd(msg_sofa, &buf, size, IPC_NOWAIT)){
                    perror("Error for msgsnd .");
                    return EXIT_FAILURE;
                }
                V(semid , SOFA); // add the number in sofa.
                V(semid , S_W_LOCK); // unlock the sofa_wait lock.
                P(semid , BARBE); // in queue to get barbed.
                P(semid , LEAVE); // in queue to charge and leave
                V(semid , PEOPLE);
            }else if(msg_length(msg_wait) < 13){
                struct msgbuf buf;
                buf.id = getpid();
                if(msgsnd(msg_wait, &buf, size, IPC_NOWAIT)){
                    perror("Error for msgsnd .");
                    return EXIT_FAILURE;
                }
                V(semid , WAIT); // add the number in wait.
                V(semid , S_W_LOCK); // unlock the sofa_wait lock.
                P(semid , IN_SOFA); // in queue to get the sofa.
                P(semid , S_W_LOCK);// imatate the value of before
                V(semid , SOFA); // add the number in sofa.
                V(semid , S_W_LOCK);// imagetate the vlaue of before.
                P(semid , BARBE); // in queue to get the barbed.
                P(semid , LEAVE); // in queue to charge and leave.
                V(semid , PEOPLE);
            }else{
                perror("Error for process the logic . \n");
                exit(EXIT_FAILURE);
            }
        }
        return EXIT_FAILURE;
    }else{
        //code for father
        waitpid(son , 0 , 0);
        return EXIT_FAILURE;
    }
    return EXIT_SUCCESS;
}

int P(int semid , int index){
    struct sembuf buf = {index , -1 , SEM_UNDO};
    if(semop(semid , &buf , 1)){
        perror("Error for operation P .");
        return EXIT_FAILURE;
    }
    return EXIT_SUCCESS;
}

int V(int semid , int index){
    struct sembuf buf = {index , 1 , SEM_UNDO};
    if(semop(semid , &buf , 1)){
        perror("Error for operation V .");
        return EXIT_FAILURE;
    }
    return EXIT_SUCCESS;
}

int msg_length(key_t msgid){
    struct msqid_ds buf;
    if(msgctl(msgid, IPC_STAT, &buf)){
        perror("Error for msgctl in msg_length.");
        return EXIT_FAILURE;
    }
    return buf.msg_qnum;
}

#endif
