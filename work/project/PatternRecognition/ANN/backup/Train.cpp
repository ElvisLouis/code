#include "Ann.cpp"

double const static THRESHOLD = 0.8;
bool start = false;
int temp_err_train = 0, temp_err_check = 0, best_err_train = 1000, best_err_check = 1000;

template <typename T>
bool check(Ann<T> *& ann , double ** set , int number , int size){
    int limit = number * THRESHOLD;
    temp_err_train = temp_err_check = 0;
    for (int i = 0; i < limit; ++i)
    {
        //cout << "seting ." << endl;
        ann -> set_input(set[i]);
        ann -> calculator();
        //cout << "calculator " << endl;
        //cout << ann -> get_result(set[i][size-1]) << endl;
        if(!ann -> get_result(set[i][size-1])){
            temp_err_train++;
        }
    }
    for (int i = limit; i < number; ++i)
    {
        ann -> set_input(set[i]);
        ann -> calculator();
        if(!ann -> get_result(set[i][size-1])){
            temp_err_check++;
        }
    }

    cout << "Get the temp_err_train for : " << temp_err_train << endl;
    cout << "Get the temp_err_check for : " << temp_err_check << endl;
    cout << "Get the best_err_train for : " << best_err_train << endl;
    cout << "Get the best_err_check for : " << best_err_check << endl;

    if (temp_err_train * THRESHOLD + temp_err_check * (1-THRESHOLD) < best_err_train * THRESHOLD + best_err_check * (1-THRESHOLD)){
        //cout << "return for true" << endl;
        best_err_train = temp_err_train;
        best_err_check = temp_err_check;
        return true;
    }
    return false;
}

template <typename T>
void train(Ann<T> * & ann , double ** set , int number , int size){
    long step = 0;
    //int limit = number * THRESHOLD;
    double * array = new double[ann -> get_argc_output()]();
    while(true){
        for (int i = 0; i < number; ++i)
        {
            int choose = rand() % number;
            cout << "The target you choose is : " << choose << endl;
            //cout << "Goal is : " << set[choose][size-1] << endl;
            array[int(round(set[choose][size-1]))] = 1;
            while(ann -> train(set[choose] ,array) != set[choose][size-1]){
                ann -> adjust_output(array);
                //ann -> print_output();
                if(ann -> output[0] != ann -> output[0]) {
                    ann -> print_output();
                    cout << "stop for error ." << endl;
                    ann -> write_file("Backup");
                    exit(1);
                }
            }
            ++step;
            if(check(ann , set , number , size)){
                cout << "save the weight . " << endl;
                ann -> save();
            }
            array[(int)(round)(set[choose][size-1])] = 0;
            cout << "step : " << step << endl;
        }
        //cout << "Running for check . " << endl;
        //if(step % 10000 == 0) cout << "step -> : " << step << endl;
        if(step >= 200000) break;
    }
    ann -> write_file("Backup");
    delete array;
}

int getNumber(){
    std :: ifstream in;
    string filename = "sample.sonic";

    in.open(filename.c_str());

    long count = 0;
    double var;
    double temp = 1 ;
    //in >> std :: fixed >> std :: setprecision(19);
    while(!in.eof()){
        in >> var;
        //cout << var * 100 << endl;
        temp = var;
        count ++;
    }

    in.close();

    count -= 1;

    cout << "Number for : " << count << endl;
    if(count % 202 == 0) {
        cout << "%202 done " << endl;
        cout << "congratulation . " << endl;
    }else{
        cout << "GG" << endl;
    }

    return count;
}

void getData(double ** & set , int & number , int & size){

    double var;
    std :: ifstream in;
    string filename = "sample.sonic";

    in.open(filename.c_str());
    if(!in.is_open()){
        cout << "Error for file busy . " << endl;
        throw "Resources busy . ";
        return;
    }

    int count = getNumber();
    number = count / 202;
    size = 202;

    set = new double*[count];

    for (int i = 0; i < count; ++i)
    {
        set[i] = new double[size];
    }
    in >> std :: fixed >> std :: setprecision(19);
    for(int k = 0 ; k < count ;){
        for (int i = 0; i < number; ++i)
        {
            for (int j = 0; j < size; ++j)
            {
                in >> var;
                if(j == size - 1){
                    //set[i][(int)round(j+var)] = 1;
                    set[i][j] = var;
                    //cout << "set[" << i << "]" << "[" << (int)round(j+var) << "]" << "\t : " << set[i][(int)round(j+var)] << endl;
                    ++k;
                }else{
                    //set[i][j] = j == size - 2 ? var : var * 100;
                    set[i][j] = var;
                }
                //cout << "set[" << i << "]" << "[" << j << "]" << "\t : " << set[i][j] << endl;
                ++k;
            }
        }
    }
    cout << "Data get done " << endl;
}

int main(int argc , char ** argv){
    cout << "Argc : " << argc << "\t argv's address : " << argv << endl;
    //int * array = new int [2]{30 , 30};
    int array[2] = {30,30};
    Ann<double> * ann = new Ann<double>(201 , 2 , array , 6 , 1e-2 , 1e-2);
    double ** set;
    int number , size;
    getData(set , number , size);
    cout << "Number for : " << number << endl;
    cout << "Size for : " << size << endl;
    ann -> train(set , number , 10000);
    cout << "Train done ." << endl;
//    train(ann , set , number , size);
    /*
      check(ann,set,number,size);
      ann -> save();
      ann -> write_file("Result.DAT");
    */
    for (int i = 0; i < number; ++i)
        delete set[i];
    delete set;
    return 0;
}